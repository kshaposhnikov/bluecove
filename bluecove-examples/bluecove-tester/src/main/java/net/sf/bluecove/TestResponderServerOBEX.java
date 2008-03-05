/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package net.sf.bluecove;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.IOUtils;
import net.sf.bluecove.util.StringUtils;
import net.sf.bluecove.util.TimeUtils;

public class TestResponderServerOBEX implements Runnable {

	private SessionNotifier serverConnection;

	private boolean isStoped = false;

	private boolean isRunning = false;

	private Thread thread;

	private TestResponderServerOBEX() {

	}

	public static TestResponderServerOBEX startServer() {
		TestResponderServerOBEX srv = new TestResponderServerOBEX();
		srv.thread = Configuration.cldcStub.createNamedThread(srv, "ServerOBEX");
		srv.thread.start();
		return srv;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void run() {
		isStoped = false;
		boolean deviceServiceClassesUpdated = false;
		LocalDevice localDevice;
		try {
			localDevice = LocalDevice.getLocalDevice();
			if (Configuration.testServerOBEX_TCP) {
				serverConnection = (SessionNotifier) Connector
						.open(BluetoothTypesInfo.PROTOCOL_SCHEME_TCP_OBEX + "://");
			} else {
				serverConnection = (SessionNotifier) Connector.open(BluetoothTypesInfo.PROTOCOL_SCHEME_BT_OBEX
						+ "://localhost:" + Configuration.blueCoveOBEXUUID() + ";name=" + Consts.RESPONDER_SERVERNAME
						+ "_ox" + Configuration.serverURLParams());
				if (Configuration.testServiceAttributes.booleanValue()) {
					ServiceRecord record = localDevice.getRecord(serverConnection);
					if (record == null) {
						Logger.warn("Bluetooth ServiceRecord is null");
					} else {
						TestResponderServer.buildServiceRecord(record);

						try {
							record.setDeviceServiceClasses(BluetoothTypesInfo.DeviceClassConsts.INFORMATION_SERVICE);
							deviceServiceClassesUpdated = true;
						} catch (Throwable e) {
							Logger.error("setDeviceServiceClasses", e);
						}

						try {
							LocalDevice.getLocalDevice().updateRecord(record);
							Logger.debug("OBEX ServiceRecord updated");
						} catch (Throwable e) {
							Logger.error("OBEX Service Record update error", e);
						}
					}
				}
			}
		} catch (Throwable e) {
			Logger.error("OBEX Server start error", e);
			isStoped = true;
			return;
		}

		if (deviceServiceClassesUpdated) {
			Logger.info("DeviceClass:" + BluetoothTypesInfo.toString(localDevice.getDeviceClass()));
		}

		try {
			int errorCount = 0;
			int count = 0;
			isRunning = true;
			boolean showServiceRecordOnce = true;
			while (!isStoped) {
				RequestHandler handler = new RequestHandler();
				try {
					count++;
					Logger.info("Accepting OBEX connections");
					if (showServiceRecordOnce) {
						Logger.debug("Url:"
								+ localDevice.getRecord(serverConnection).getConnectionURL(
										ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false));
						showServiceRecordOnce = false;
					}
					if (Configuration.authenticateOBEX.getValue() != 0) {
						handler.auth = new OBEXTestAuthenticator("server" + count);
						handler.connectionAccepted(serverConnection.acceptAndOpen(handler, handler.auth));
					} else {
						handler.connectionAccepted(serverConnection.acceptAndOpen(handler));
					}
				} catch (InterruptedIOException e) {
					isStoped = true;
					break;
				} catch (Throwable e) {
					if (errorCount > 3) {
						isStoped = true;
					}
					if (isStoped) {
						return;
					}
					errorCount++;
					Logger.error("acceptAndOpen ", e);
					continue;
				}
				errorCount = 0;
			}
		} finally {
			close();
			Logger.info("OBEX Server finished! " + TimeUtils.timeNowToString());
			isRunning = false;
		}
	}

	void close() {
		try {
			if (serverConnection != null) {
				serverConnection.close();
			}
			Logger.debug("OBEX ServerConnection closed");
		} catch (Throwable e) {
			Logger.error("OBEX Server stop error", e);
		}
	}

	void closeServer() {
		isStoped = true;
		close();
	}

	/*
	 * We testing on Java 1.1 and Timer is not important
	 */
	private class NoTimeWrapper {

		Object timer;

		NoTimeWrapper() {
			try {
				timer = new Timer();
			} catch (Throwable e) {
				Logger.warn("OBEX Server has no timer");
			}
		}

		void schedule(final RequestHandler handler) {
			if (timer != null) {
				((Timer) timer).schedule(new TimerTask() {
					public void run() {
						handler.notConnectedClose();
					}
				}, 1000 * 30);
			}
		}

		void cancel() {
			if (timer != null) {
				((Timer) timer).cancel();
			}
		}
	}

	private class RequestHandler extends ServerRequestHandler {

		OBEXTestAuthenticator auth;

		NoTimeWrapper notConnectedTimer = new NoTimeWrapper();

		boolean isConnected = false;

		Connection cconn;

		RemoteDevice remoteDevice;

		void connectionAccepted(Connection cconn) {
			Logger.info("Received OBEX connection");
			this.cconn = cconn;
			if (!Configuration.testServerOBEX_TCP) {
				try {
					remoteDevice = RemoteDevice.getRemoteDevice(cconn);
					Logger.debug("connected toBTAddress " + remoteDevice.getBluetoothAddress());
				} catch (IOException e) {
					Logger.error("OBEX Server error", e);
				}
			}
			if (!isConnected) {
				notConnectedTimer.schedule(this);
			}
		}

		void notConnectedClose() {
			if (!isConnected) {
				Logger.debug("OBEX connection timeout");
				IOUtils.closeQuietly(cconn);
			}
		}

		public int onConnect(HeaderSet request, HeaderSet reply) {
			isConnected = true;
			notConnectedTimer.cancel();
			Logger.debug("OBEX onConnect");
			if (Configuration.authenticate.booleanValue()) {
				if (!remoteDevice.isAuthenticated()) {
					return ResponseCodes.OBEX_HTTP_FORBIDDEN;
				}
				Logger.debug("OBEX connection Authenticated");
			}
			return ResponseCodes.OBEX_HTTP_OK;
		}

		public void onDisconnect(HeaderSet request, HeaderSet reply) {
			Logger.debug("OBEX onDisconnect");
		}

		public int onSetPath(HeaderSet request, HeaderSet reply, boolean backup, boolean create) {
			Logger.debug("OBEX onSetPath");
			return super.onSetPath(request, reply, backup, create);
		}

		public int onDelete(HeaderSet request, HeaderSet reply) {
			Logger.debug("OBEX onDelete");
			return super.onDelete(request, reply);
		}

		public int onPut(Operation op) {
			Logger.debug("OBEX onPut");
			try {
				HeaderSet hs = op.getReceivedHeaders();
				String name = (String) hs.getHeader(HeaderSet.NAME);
				if (name != null) {
					Logger.debug("name:" + name);

					HeaderSet sendHeaders = createHeaderSet();
					sendHeaders.setHeader(HeaderSet.DESCRIPTION, name);
					op.sendHeaders(sendHeaders);
				}

				InputStream is = op.openInputStream();

				StringBuffer buf = new StringBuffer();
				while (!isStoped) {
					int data = is.read();
					if (data == -1) {
						Logger.debug("EOS recived");
						break;
					}
					char c = (char) data;
					buf.append(c);
					if ((c == '\n') || (buf.length() > 30)) {
						Logger.debug("cc:" + StringUtils.toBinaryText(buf));
						buf = new StringBuffer();
					}
				}
				if (buf.length() > 0) {
					Logger.debug("cc:" + StringUtils.toBinaryText(buf));
				}
				op.close();
				return ResponseCodes.OBEX_HTTP_OK;
			} catch (IOException e) {
				Logger.error("OBEX Server onPut error", e);
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			} finally {
				Logger.debug("OBEX onPut ends");
			}
		}

		public int onGet(Operation op) {
			Logger.debug("OBEX onGet");
			String message = "Hello client! now " + new Date().toString();
			try {
				HeaderSet hs = op.getReceivedHeaders();
				String name = (String) hs.getHeader(HeaderSet.NAME);

				if (name != null) {
					message += "\nYou ask for [" + name + "]";
				}
				if (name != null) {
					HeaderSet sendHeaders = createHeaderSet();
					sendHeaders.setHeader(HeaderSet.DESCRIPTION, name);
					op.sendHeaders(sendHeaders);
				}
				byte[] messageBytes = message.getBytes();

				OutputStream os = op.openOutputStream();
				os.write(messageBytes);
				os.flush();
				os.close();
				op.close();
				return ResponseCodes.OBEX_HTTP_OK;
			} catch (IOException e) {
				Logger.error("OBEX Server onGet error", e);
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			} finally {
				Logger.debug("OBEX onGet ends");
			}
		}

		public void onAuthenticationFailure(byte[] userName) {
			Logger.debug("OBEX AuthFailure " + new String(userName));
		}

	}
}