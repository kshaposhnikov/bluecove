/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
package com.intel.bluetooth;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

class BluetoothStreamConnectionNotifier extends BluetoothConnectionNotifierBase implements StreamConnectionNotifier {

	private int rfcommChannel = -1;

	public BluetoothStreamConnectionNotifier(BluetoothConnectionNotifierParams params) throws IOException {
		super(params);

		this.handle = BlueCoveImpl.instance().getBluetoothStack().rfServerOpen(params, serviceRecord);

		this.rfcommChannel = serviceRecord.getChannel(BluetoothConsts.RFCOMM_PROTOCOL_UUID);

		this.serviceRecord.attributeUpdated = false;

		this.securityOpt = Utils.securityOpt(params.authenticate, params.encrypt);
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#closeStack(long)
	 */
	protected void closeStack(long handle) throws IOException {
		BlueCoveImpl.instance().getBluetoothStack().rfServerClose(handle, serviceRecord);
	}


	/*
	 * Returns a StreamConnection that represents a server side socket
	 * connection. Returns: A socket to communicate with a client. Throws:
	 * IOException - If an I/O error occurs.
	 */

	public StreamConnection acceptAndOpen() throws IOException {
		if (closed) {
			throw new IOException("Notifier is closed");
		}
		if (((ServiceRecordImpl) serviceRecord).attributeUpdated) {
			updateServiceRecord(true);
		}
		try {
			long clientHandle = BlueCoveImpl.instance().getBluetoothStack().rfServerAcceptAndOpenRfServerConnection(handle);
			int clientSecurityOpt = BlueCoveImpl.instance().getBluetoothStack().getSecurityOpt(clientHandle, this.securityOpt);
			return new BluetoothRFCommServerConnection(clientHandle, clientSecurityOpt);
		} catch (IOException e) {
			if (closed || closing) {
				throw new InterruptedIOException("Notifier has been closed");
			}
			throw e;
		}
	}

	protected void validateServiceRecord(ServiceRecord srvRecord) {
		if (this.rfcommChannel != serviceRecord.getChannel(BluetoothConsts.RFCOMM_PROTOCOL_UUID)) {
			throw new IllegalArgumentException("Must not change the RFCOMM server channel number");
		}
		super.validateServiceRecord(srvRecord);
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#updateStackServiceRecord(com.intel.bluetooth.ServiceRecordImpl, boolean)
	 */
	protected void updateStackServiceRecord(ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
		BlueCoveImpl.instance().getBluetoothStack().rfServerUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
	}

}