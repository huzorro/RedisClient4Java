package org.elk.redis4j.impl.transfers;

import java.util.concurrent.Executors;

import org.elk.redis4j.api.ISession;
import org.elk.redis4j.api.Sharding;
import org.elk.redis4j.impl.transfers.decoder.FrameToObjectArray;
import org.elk.redis4j.impl.transfers.decoder.InputStreamToFrame;
import org.elk.redis4j.impl.transfers.encoder.CommandWrapperToBinaryData;
import org.elk.redis4j.impl.transfers.handler.HeartbeatHandler;
import org.elk.redis4j.impl.transfers.handler.MessageHandler;
import org.elk.redis4j.impl.transfers.handler.ReconnectNetworkHandler;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;


public class SessionManager
{
	private ChannelGroup channelGroup;
	private ClientBootstrap bootstrap;
	private final Timer timer = new HashedWheelTimer();

	public SessionManager()
	{
		this.bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
		this.channelGroup = new DefaultChannelGroup();
	}

	public ISession createSession(Sharding sharding)
	{
		ISession session = new Session(this, sharding);

		ChannelPipeline pipeline = Channels.pipeline();

		pipeline.addLast("IdleStateHandler", new IdleStateHandler(timer, 0, session.getHeartbeatTime(), 0));
		if (session.isUseHeartbeat())
		{
			pipeline.addLast("HeartbeatHandler", new HeartbeatHandler(session));
		}
		pipeline.addLast("ReconnectNetworkHandler", new ReconnectNetworkHandler(session, timer));

		pipeline.addLast("ObjectEncoder", new CommandWrapperToBinaryData());

		pipeline.addLast("FramerDecoder", new InputStreamToFrame());
		pipeline.addLast("ObjectDecoder", new FrameToObjectArray());

		pipeline.addLast("MessageHandler", new MessageHandler(session));

		this.bootstrap.setPipeline(pipeline);
		session.setPipiline(pipeline);

		ChannelFuture future = this.bootstrap.connect(sharding.getServerAddress());

		Channel channel = future.awaitUninterruptibly().getChannel();
		if (future.isSuccess())
		{
			this.channelGroup.add(channel);

			NioSocketChannelConfig cfg = (NioSocketChannelConfig) channel.getConfig();
			cfg.setSendBufferSize(32*1024);
			cfg.setReceiveBufferSize(64*1024);
//			System.out.println("isTcpNoDelay = " + cfg.isTcpNoDelay());
//			System.out.println("isReuseAddress = " + cfg.isReuseAddress());
//			System.out.println("isKeepAlive = " + cfg.isKeepAlive());
//			System.out.println("getReceiveBufferSize = " + cfg.getReceiveBufferSize());
//			System.out.println("getSendBufferSize = " + cfg.getSendBufferSize());
//			System.out.println("getSoLinger = " + cfg.getSoLinger());
//			System.out.println("getWriteSpinCount = " + cfg.getWriteSpinCount());

			session.setChannel(channel);
		}
		return session;
	}

	public synchronized void updateChannel(ISession session)
	{
		this.bootstrap.setPipeline(session.getPipiline());
		ChannelFuture future = this.bootstrap.connect(session.getRemoteAddress());
		Channel channel = future.awaitUninterruptibly().getChannel();
		if (future.isSuccess())
		{
			this.channelGroup.add(channel);
			session.setChannel(channel);
		}
	}

	public void disconnectAllSession()
	{
		this.timer.stop();
		this.channelGroup.disconnect().awaitUninterruptibly();
		this.channelGroup.close().awaitUninterruptibly();
		this.bootstrap.releaseExternalResources();
	}
}
