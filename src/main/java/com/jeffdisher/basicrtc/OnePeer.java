package com.jeffdisher.basicrtc;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Duration;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.jeffdisher.breakwater.utilities.Assert;


public class OnePeer implements WebSocketListener
{
	private final boolean _isVerbose;
	private final String _roomName;
	private final IPeerRegistry _registry;
	private Session _thisPeer;
	private SocketAddress _thisPeerAddress;
	private OnePeer _otherPeer;

	public OnePeer(boolean isVerbose, String roomName, IPeerRegistry registry)
	{
		_isVerbose = isVerbose;
		_roomName = roomName;
		_registry = registry;
	}

	/**
	 * Called on the first peer in a pair after the second ("other") peer has connected to attach them.
	 * 
	 * @param otherPeer The other peer.
	 */
	public void attachOtherPeer(OnePeer otherPeer)
	{
		// We must have already connected.
		Assert.assertTrue(null != _thisPeer);
		Assert.assertTrue(null == _otherPeer);
		_otherPeer = otherPeer;
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason)
	{
		// We must have already connected.
		Assert.assertTrue(null != _thisPeer);
		// Unregister us - this does nothing if we didn't register the room or we were alread paired.
		_registry.disconnectPeer(_roomName, this);
		// If there is an other peer, close them, too.
		// NOTE:  These callbacks MUST be serialized so this can't be racy.
		if (null != _otherPeer)
		{
			_otherPeer._thisPeer.close();
		}
		if (_isVerbose)
		{
			System.out.println("Disconnect in " + _roomName + ": " + _thisPeerAddress);
		}
	}

	@Override
	public void onWebSocketConnect(Session session)
	{
		Assert.assertTrue(null == _thisPeer);
		_thisPeer = session;
		// We cache the peer address since it seems to sometimes be cleared in disconnect.
		_thisPeerAddress = _thisPeer.getRemoteAddress();
		if (_isVerbose)
		{
			System.out.println("Connect in " + _roomName + ": " + _thisPeerAddress);
		}
		
		// Set the timeout to 30 minutes - should be reasonable for this use-case.
		session.setIdleTimeout(Duration.ofMinutes(30));
		
		// We will try to register and see if we find a peer.
		// Note that this could race with other attempts to set _otherPeer, so check in a local first since we will never see it change _to_ null.
		OnePeer possiblePeer = _registry.connectPeer(_roomName, this);
		if (null != possiblePeer)
		{
			Assert.assertTrue(null == _otherPeer);
			_otherPeer = possiblePeer;
			
			// This means that we are the second peer, meaning we need to send the "start" message.
			JsonObject object = new JsonObject();
			object.add("type", "start");
			object.add("payload", Json.NULL);
			try
			{
				_thisPeer.getRemote().sendString(object.toString());
			}
			catch (IOException e)
			{
				// TODO:  Determine how this happens so we can fix it.
				throw Assert.unexpected(e);
			}
		}
	}

	@Override
	public void onWebSocketError(Throwable cause)
	{
		// TODO:  Determine how this happens so we can fix it.
		throw Assert.unexpected(cause);
	}

	@Override
	public void onWebSocketText(String message)
	{
		// We don't expect any traffic on this socket until we are connected and paired.
		Assert.assertTrue(null != _thisPeer);
		Assert.assertTrue(null != _otherPeer);
		
		// We just send the message to the other side.
		if (_isVerbose)
		{
			System.out.println("Message in " + _roomName + ": " + message);
		}
		try
		{
			_otherPeer._thisPeer.getRemote().sendString(message);
		}
		catch (IOException e)
		{
			// TODO:  Determine how this happens so we can fix it.
			throw Assert.unexpected(e);
		}
	}


	public interface IPeerRegistry
	{
		void disconnectPeer(String roomName, OnePeer peer);
		OnePeer connectPeer(String roomName, OnePeer peer);
	}
}
