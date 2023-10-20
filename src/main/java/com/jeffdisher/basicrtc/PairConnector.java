package com.jeffdisher.basicrtc;

import java.util.HashMap;
import java.util.Map;


/**
 * This class maintains a mapping of partial pairs of objects while waiting for the second part of the pair to arrive
 * or the first to be removed.  The pairs are represented by name.
 * The interface is synchronized since it is assumed that multiple threads will be adding/removing/connecting pairs,
 * concurrently.
 * 
 * @param <T> The pair data type.
 */
public class PairConnector<T>
{
	private final IPairConnector<T> _connector;
	private final Map<String, T> _partialPairs;

	/**
	 * Creates a new empty instance.
	 * 
	 * @param connector The utility to use when connecting pairs under the internal lock.
	 */
	public PairConnector(IPairConnector<T> connector)
	{
		_connector = connector;
		_partialPairs = new HashMap<>();
	}

	/**
	 * If there is still a partial match for pairName, and the pair is the same instance as instanceToMatch, it is
	 * removed.
	 * 
	 * @param pairName
	 * @param instanceToMatch
	 * @return True if the match was removed.
	 */
	public synchronized boolean removePartialIfMatched(String pairName, T instanceToMatch)
	{
		boolean wasRemoved = false;
		T registered = _partialPairs.get(pairName);
		if (instanceToMatch == registered)
		{
			_partialPairs.remove(pairName);
			wasRemoved = true;
		}
		return wasRemoved;
	}

	/**
	 * Attempts to install a new partial pair instance but will instead connect it to an existing instance, if there
	 * already was a partial pair for the given name.
	 * 
	 * @param pairName The name of the pair to install/connect.
	 * @param possibleInstance The partial pair instance we want to install or connect to an existing pair (as the
	 * second part).
	 * @return The first part of the now-complete pair, if there already was one, or null if possibleInstance was
	 * installed as a partial pair.
	 */
	public synchronized T attachOrRegisterPartial(String pairName, T possibleInstance)
	{
		// See if there is already a peer registered.  If so, remove it from the map, connect it to this new peer, and return it.
		T first = _partialPairs.remove(pairName);
		if (null != first)
		{
			// Since we found the partial pair for this name (and removed it), we want to attach it before releasing lock.
			_connector.attachPair(first, possibleInstance);
		}
		else
		{
			// This is the first part of the pair so just install it.
			_partialPairs.put(pairName, possibleInstance);
		}
		return first;
	}


	/**
	 * Since we want to connect the pairs under lock (to avoid races on attach/disconnect), we use this interface so
	 * that the caller can provide the connection mechanism while this class remains fully abstract.
	 * 
	 * @param <T> The pair data type.
	 */
	public interface IPairConnector<T>
	{
		/**
		 * Connects the 2 given pairs together.
		 * 
		 * @param first The "first" part of the pair.
		 * @param second The "second" part of the pair.
		 */
		void attachPair(T first, T second);
	}
}
