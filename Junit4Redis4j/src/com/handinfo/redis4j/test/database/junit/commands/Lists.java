package com.handinfo.redis4j.test.database.junit.commands;

import org.junit.Test;

import com.handinfo.redis4j.api.ListPosition;
import com.handinfo.redis4j.api.database.IRedisDatabaseClient;
import com.handinfo.redis4j.api.exception.ErrorCommandException;
import com.handinfo.redis4j.test.Helper;
import com.handinfo.redis4j.test.database.junit.RedisCommandTestBase;

public class Lists extends RedisCommandTestBase
{
	@Test
	public void blpop() throws InterruptedException
	{
		String[] result = client.listBlockLeftPop(1, "foo");
		assertNull(result);

		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					IRedisDatabaseClient cli = Helper.getRedisDatabaseClient();
					Integer res = cli.listLeftPush("foo", "bar");
					cli.quit();
					assertEquals(1, res.intValue());
				}
				catch (Exception ex)
				{
					fail(ex.getMessage());
				}
			}
		}).start();

		result = client.listBlockLeftPop(1, "foo");
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("foo", result[0]);
		assertEquals("bar", result[1]);
	}
	
	@Test
	public void brpop() throws InterruptedException
	{
		String[] result = client.listBlockRightPop(1, "foo");
		assertNull(result);

		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					IRedisDatabaseClient cli = Helper.getRedisDatabaseClient();
					Integer res = cli.listLeftPush("foo", "bar");
					cli.quit();
					assertEquals(1, res.intValue());
				}
				catch (Exception ex)
				{
					fail(ex.getMessage());
				}
			}
		}).start();

		result = client.listBlockRightPop(1, "foo");
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("foo", result[0]);
		assertEquals("bar", result[1]);
	}
	
	@Test
	public void brpoplpush()
	{
		(new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					Thread.sleep(2000);
					IRedisDatabaseClient cli = Helper.getRedisDatabaseClient();
					Integer res = cli.listLeftPush("foo", "a");
					cli.quit();
					assertEquals(1, res.intValue());
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		})).start();

		String element = client.listBlockRightPopLeftPush("foo", "bar", 0);

		assertEquals("a", element);
		assertEquals(1, client.listLength("bar").intValue());
		assertEquals("a", client.listRange("bar", 0, -1)[0]);
	}
	
	@Test
	public void lindex()
	{
		client.listLeftPush("foo", "1");
		client.listLeftPush("foo", "2");
		client.listLeftPush("foo", "3");

		String[] expected = new String[3];
		expected[0] = "3";
		expected[1] = "bar";
		expected[2] = "1";

		Boolean status = client.listSet("foo", 1, "bar");
		assertEquals(true, status);
		assertEquals(expected, client.listRange("foo", 0, 100));
	}
	
	@Test
	public void linsert()
	{
		Integer status = client.listLeftInsert("foo", ListPosition.BEFORE, "bar", "car");
		assertEquals(0, status.intValue());

		client.listLeftPush("foo", "a");
		status = client.listLeftInsert("foo", ListPosition.AFTER, "a", "b");
		assertEquals(2, status.intValue());

		String[] actual = client.listRange("foo", 0, 100);
		String[] expected = new String[2];
		expected[0] = "a";
		expected[1] = "b";

		assertEquals(expected, actual);

		status = client.listLeftInsert("foo", ListPosition.BEFORE, "bar", "car");
		assertEquals(-1, status.intValue());
	}
	
	@Test
	public void llen()
	{
		assertEquals(0, client.listLength("foo").intValue());
		client.listLeftPush("foo", "bar");
		client.listLeftPush("foo", "car");
		assertEquals(2, client.listLength("foo").intValue());
	}
	
	@Test(expected = ErrorCommandException.class)
	public void llenNotOnList()
	{
		client.set("foo", "bar");
		client.listLength("foo");
	}

	@Test
	public void lpop()
	{
		client.listRightPush("foo", "a");
		client.listRightPush("foo", "b");
		client.listRightPush("foo", "c");

		String element = client.listLeftPop("foo");
		assertEquals("a", element);

		String[] expected = new String[2];
		expected[0] = "b";
		expected[1] = "c";

		assertEquals(expected, client.listRange("foo", 0, 1000));
		client.listLeftPop("foo");
		client.listLeftPop("foo");

		element = client.listLeftPop("foo");
		assertEquals(null, element);
	}
	
	@Test
	public void lpush()
	{
		Integer size = client.listLeftPush("foo", "bar");
		assertEquals(1, size.intValue());
		size = client.listLeftPush("foo", "foo");
		assertEquals(2, size.intValue());
	}
	
	@Test
	public void lpushx()
	{
		Integer status = client.listLeftPushOnExist("foo", "bar");
		assertEquals(0, status.intValue());

		client.listLeftPush("foo", "a");
		status = client.listLeftPushOnExist("foo", "b");
		assertEquals(2, status.intValue());
	}
	
	@Test
	public void lrange()
	{
		client.listRightPush("foo", "a");
		client.listRightPush("foo", "b");
		client.listRightPush("foo", "c");

		String[] expected = new String[3];
		expected[0] = "a";
		expected[1] = "b";
		expected[2] = "c";

		String[] range = client.listRange("foo", 0, 2);
		assertEquals(expected, range);

		range = client.listRange("foo", 0, 20);
		assertEquals(expected, range);

		expected = new String[2];
		expected[0] = "b";
		expected[1] = "c";

		range = client.listRange("foo", 1, 2);
		assertEquals(expected, range);

		expected = new String[0];
		range = client.listRange("foo", 2, 1);
		assertEquals(expected, range);
	}
	
	@Test
	public void lrem()
	{
		client.listLeftPush("foo", "hello");
		client.listLeftPush("foo", "hello");
		client.listLeftPush("foo", "x");
		client.listLeftPush("foo", "hello");
		client.listLeftPush("foo", "c");
		client.listLeftPush("foo", "b");
		client.listLeftPush("foo", "a");

		Integer count = client.listRemove("foo", -2, "hello");
		assertEquals(2, count.intValue());

		String[] expected = new String[5];
		expected[0] = "a";
		expected[1] = "b";
		expected[2] = "c";
		expected[3] = "hello";
		expected[4] = "x";
		
		assertEquals(expected, client.listRange("foo", 0, 1000));
		assertEquals(0, client.listRemove("bar", 100, "foo").intValue());
	}

	@Test
	public void lset()
	{
		client.listLeftPush("foo", "1");
		client.listLeftPush("foo", "2");
		client.listLeftPush("foo", "3");

		assertEquals("3", client.listIndex("foo", 0));
		assertEquals(null, client.listIndex("foo", 100));
	}
	
	@Test
	public void ltrim()
	{
		client.listLeftPush("foo", "1");
		client.listLeftPush("foo", "2");
		client.listLeftPush("foo", "3");
		Boolean status = client.listTrim("foo", 0, 1);
		assertEquals(true, status);

		String[] expected = new String[2];
		expected[0] = "3";
		expected[1] = "2";
		assertEquals(2, client.listLength("foo").intValue());
		assertEquals(expected, client.listRange("foo", 0, 100));
	}

	@Test
	public void rpop()
	{
		client.listRightPush("foo", "a");
		client.listRightPush("foo", "b");
		client.listRightPush("foo", "c");

		String element = client.listRightPop("foo");
		assertEquals("c", element);

		String[] expected = new String[2];
		expected[0] = "a";
		expected[1] = "b";

		assertEquals(expected, client.listRange("foo", 0, 1000));
		client.listRightPop("foo");
		client.listRightPop("foo");

		element = client.listRightPop("foo");
		assertEquals(null, element);
	}
	
	@Test
	public void rpoplpush()
	{
		client.listRightPush("foo", "a");
		client.listRightPush("foo", "b");
		client.listRightPush("foo", "c");

		client.listRightPush("dst", "foo");
		client.listRightPush("dst", "bar");

		String element = client.listRightPopLeftPush("foo", "dst");

		assertEquals("c", element);

		String[] srcExpected = new String[2];
		srcExpected[0] = "a";
		srcExpected[1] = "b";

		String[] dstExpected = new String[3];
		dstExpected[0] = "c";
		dstExpected[1] = "foo";
		dstExpected[2] = "bar";


		assertEquals(srcExpected, client.listRange("foo", 0, 1000));
		assertEquals(dstExpected, client.listRange("dst", 0, 1000));
	}
	
	@Test
	public void rpush()
	{
		Integer size = client.listRightPush("foo", "bar");
		assertEquals(1, size.intValue());
		size = client.listRightPush("foo", "foo");
		assertEquals(2, size.intValue());
	}
	
	@Test
	public void rpushx()
	{
		Integer status = client.listRightPushOnExist("foo", "bar");
		assertEquals(0, status.intValue());

		client.listLeftPush("foo", "a");
		status = client.listRightPushOnExist("foo", "b");
		assertEquals(2, status.intValue());
	}
}