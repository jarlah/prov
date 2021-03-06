package com.owera.xaps.base.db;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.owera.xaps.base.BaseCache;
import com.owera.xaps.base.Log;
import com.owera.xaps.dbi.File;
import com.owera.xaps.dbi.Profile;
import com.owera.xaps.dbi.Unit;
import com.owera.xaps.dbi.UnitJob;
import com.owera.xaps.dbi.UnitJobs;
import com.owera.xaps.dbi.UnitParameter;

public class DBAccessStatic {

	private static void debug(String message) {
		Log.debug(DBAccessStatic.class, message);
	}

	private static void error(String message) {
		Log.error(DBAccessStatic.class, message);
	}

	public static byte[] readFirmwareImage(File firmwareFresh) throws SQLException {
		long start = System.currentTimeMillis();
		String action = "readFirmwareImage";
		try {
			File firmwareCache = BaseCache.getFirmware(firmwareFresh.getName(), firmwareFresh.getUnittype().getName());
			File firmwareReturn = null;
			if (firmwareCache != null && firmwareFresh != null && firmwareFresh.getId() == firmwareCache.getId())
				firmwareReturn = firmwareCache;
			else {
				firmwareFresh.setBytes(firmwareFresh.getContent());
				BaseCache.putFirmware(firmwareFresh.getName(), firmwareFresh.getUnittype().getName(), firmwareFresh);
				firmwareReturn = firmwareFresh;
			}
			return firmwareReturn.getContent();
		} catch (Throwable t) {
			DBAccess.handleError(action, start, t);
		}
		return null; // Unreachable code - compiler doesn't detect it
	}

	public static void startUnitJob(String unitId, Integer jobId) throws SQLException {
		long start = System.currentTimeMillis();
		String action = "startUnitJob";
		try {
			UnitJobs unitJobs = new UnitJobs(DBAccess.getXAPSProperties());
			UnitJob uj = new UnitJob(unitId, jobId);
			uj.setStartTimestamp(new Date());
			boolean updated = unitJobs.start(uj);
			if (updated) {
				debug("Have started unit-job (job " + jobId + ")");
			} else {
				error("The unit-job couldn't be started. The reason might it is already COMPLETED_OK state");
			}
		} catch (Throwable t) {
			DBAccess.handleError(action, start, t);
		}
	}

	public static void stopUnitJob(String unitId, Integer jobId, String unitJobStatus) throws SQLException {
		long start = System.currentTimeMillis();
		String action = "stopUnitJob";
		try {
			UnitJobs unitJobs = new UnitJobs(DBAccess.getXAPSProperties());
			UnitJob uj = new UnitJob(unitId, jobId);
			uj.setEndTimestamp(new Date());
			uj.setStatus(unitJobStatus);
			boolean stopped = unitJobs.stop(uj);
			if (stopped) {
				debug("Have stopped unit-job (job " + jobId + "), status set to " + unitJobStatus);
			} else {
				error("The unit-job couldn't be stopped. The reason might be it is deleted or maybe even in COMPLETED_OK state already");
			}
		} catch (Throwable t) {
			DBAccess.handleError(action, start, t);
		}
	}

	// Write to queue, will be written to DB at the end of TR-069-session.
	public static void queueUnitParameters(Unit unit, List<UnitParameter> unitParameters, Profile profile) {
		for (UnitParameter up : unitParameters) {
			unit.toWriteQueue(up);	
		}
	}
}
