package com.jeffdisher.basicrtc;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;


public class TestPairConnector
{
	@Test
	public void simpleConnections() throws Throwable
	{
		PairConnector<StringBuilder> connector = new PairConnector<>((StringBuilder one, StringBuilder two) -> one.append(two));
		Assert.assertNull(connector.attachOrRegisterPartial("one", new StringBuilder("A")));
		Assert.assertNull(connector.attachOrRegisterPartial("two", new StringBuilder("B")));
		Assert.assertNull(connector.attachOrRegisterPartial("three", new StringBuilder("C")));
		Assert.assertEquals("AB", connector.attachOrRegisterPartial("one", new StringBuilder("B")).toString());
		Assert.assertEquals("BC", connector.attachOrRegisterPartial("two", new StringBuilder("C")).toString());
		Assert.assertEquals("CD", connector.attachOrRegisterPartial("three", new StringBuilder("D")).toString());
		Assert.assertNull(connector.attachOrRegisterPartial("one", new StringBuilder("D")));
		Assert.assertEquals("DB", connector.attachOrRegisterPartial("one", new StringBuilder("B")).toString());
	}

	@Test
	public void concurrentConnections() throws Throwable
	{
		PairConnector<int[]> connector = new PairConnector<>((int[] one, int[] two) -> one[1] = two[0]);
		String names[] = { "A", "B", "C", "D", "E" };
		// We need to use an even number of threads so we always see them all paired.
		Thread threads[] = new Thread[4];
		AtomicInteger firsts[] = new AtomicInteger[threads.length];
		AtomicInteger seconds[] = new AtomicInteger[threads.length];
		for (int i = 0; i < threads.length; ++i)
		{
			final int thisInt = i;
			firsts[i] = new AtomicInteger(0);
			seconds[i] = new AtomicInteger(0);
			threads[i] = new Thread(()->{
				for (int j = 0; j < names.length; ++j)
				{
					int[] pair = connector.attachOrRegisterPartial(names[j], new int[] {thisInt, -1});
					if (null != pair)
					{
						firsts[pair[0]].incrementAndGet();
						seconds[pair[1]].incrementAndGet();
					}
					// We want to do a short sleep to perturb the execution order, slightly.  Otherwise, we tend to get each thread being purely in one side or another.
					try
					{
						Thread.sleep(10L);
					}
					catch (InterruptedException e)
					{
						Assert.fail("We don't use interruption");
					}
				}
			});
		}
		for (Thread thread : threads)
		{
			thread.start();
		}
		for (Thread thread : threads)
		{
			thread.join();
		}
		
		// We now check the number of firsts and seconds for every index - they should all total to names.length since each thread should appear for each name.
		for (int i = 0; i < threads.length; ++i)
		{
			Assert.assertEquals(names.length, firsts[i].get() + seconds[i].get());
		}
	}
}
