package com.handinfo.redis4j.api;



public interface IRedis4j
{
	/***********************************************************\
	 * Connection
	\***********************************************************/
	public boolean auth(String password);
	public boolean ping();
	public boolean select(int dbIndex);
	public String echo(String message);
	public boolean quit();
	
	/***********************************************************\
	 * Server
	\***********************************************************/
	
	/***********************************************************\
	 * Transactions
	\***********************************************************/
	
	/***********************************************************\
	 * Pub/Sub
	\***********************************************************/
	
	/***********************************************************\
	 * Sets
	\***********************************************************/
	
	/***********************************************************\
	 * Sorted Sets
	\***********************************************************/
	
	/***********************************************************\
	 * Lists
	\***********************************************************/
	
	/***********************************************************\
	 * Hashes
	\***********************************************************/
	
	/***********************************************************\
	 * Strings
	\***********************************************************/
	public String  get(String key);
	public boolean set(String key, String value);
	public Object getObject(String key);
	public <T> boolean setObject(String key, T value);
	
	/***********************************************************\
	 * Keys
	\***********************************************************/
	
	/***********************************************************\
	 * OTHER
	\***********************************************************/
	public boolean connect();
}
