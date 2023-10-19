package com.jeffdisher.basicrtc;

import java.io.IOException;
import java.time.Duration;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.jeffdisher.breakwater.utilities.Assert;


public class OnePeer implements WebSocketListener
{
	private final String _roomName;
	private final IPeerRegistry _registry;
	private Session _thisPeer;
	private OnePeer _otherPeer;

	public OnePeer(String roomName, IPeerRegistry registry)
	{
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
		// Unregister us - this does nothing if we were already paired.
		_registry.disconnectPeer(_roomName);
		// If there is an other peer, close them, too.
		// NOTE:  These callbacks MUST be serialized so this can't be racy.
		if (null != _otherPeer)
		{
			_otherPeer._thisPeer.close();
		}
	}

	@Override
	public void onWebSocketConnect(Session session)
	{
		Assert.assertTrue(null == _thisPeer);
		_thisPeer = session;
		
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
		void disconnectPeer(String roomName);
		OnePeer connectPeer(String roomName, OnePeer peer);
	}
}
