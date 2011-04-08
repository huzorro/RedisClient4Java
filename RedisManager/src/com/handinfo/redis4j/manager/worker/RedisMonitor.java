package com.handinfo.redis4j.manager.worker;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;

import javax.swing.SwingWorker;

import com.handinfo.redis4j.api.RedisCommand;
import com.handinfo.redis4j.api.async.IRedisAsyncClient;
import com.handinfo.redis4j.api.async.IRedisAsyncClient.Result;
import com.handinfo.redis4j.api.database.IRedisDatabaseClient;
import com.handinfo.redis4j.api.exception.CleanLockedThreadException;
import com.handinfo.redis4j.api.exception.ErrorCommandException;
import com.handinfo.redis4j.impl.RedisClientBuilder;

public class RedisMonitor extends SwingWorker<String, String>
{
	private IResult result;
	private IRedisAsyncClient client;

	public RedisMonitor(IRedisDatabaseClient client, IResult result)
	{
		this.client = RedisClientBuilder.buildAsyncClient(client.getShardInfo());
		this.result = result;
	}

	@Override
	public String doInBackground()
	{
		try
		{
			client.executeCommand(RedisCommand.MONITOR, new Result()
			{

				@Override
				public void doInCurrentThread(String result)
				{
					if (!isCancelled())
					{
						publish(result + "\n");
					}
				}
			});
		}
		catch (InterruptedException e)
		{
			//e.printStackTrace();
		}
		catch (CleanLockedThreadException e)
		{
			e.printStackTrace();
		}
		catch (ErrorCommandException e)
		{
			e.printStackTrace();
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
		}
		catch (BrokenBarrierException e)
		{
			e.printStackTrace();
		}
		finally
		{
			client.quit();
		}

		return "";
	}

	@Override
	protected void process(List<String> chunks)
	{
		String strResult = "";
		for (String str : chunks)
		{
			strResult += str;
		}
		this.result.setResult(java.util.Calendar.getInstance().getTime() + "\n" + strResult + "-------------------------------");
	}
}
