package org.bio_answerfinder.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class Profiler {
	protected Map<String, TimeStats> statsMap;
	protected String profilerName;
	protected boolean enabled = true;

	public Profiler(String profilerName) {
		this.profilerName = profilerName;
		statsMap = new HashMap<String, TimeStats>(11);
	}

	public void start(String name) {
		if (enabled) {
			TimeStats ts = statsMap.get(name);
			if (ts == null) {
				statsMap.put(name, new TimeStats(System.currentTimeMillis()));
			} else {
				ts.start = System.currentTimeMillis();
			}
		}
	}

	public void stop(String name) {
		if (!enabled) {
			return;
		}
		TimeStats ts = statsMap.get(name);
		if (ts != null) {
			ts.setEnd(System.currentTimeMillis());
		}
	}

	public String showStats() {
		if (!enabled) {
			return "";
		}
		StringBuffer buf = new StringBuffer(256);
		buf.append("Profiler Stats for ").append(profilerName).append("\n");
		for (Map.Entry<String, TimeStats> entry : statsMap.entrySet()) {
			String name = (String) entry.getKey();
			TimeStats ts = (TimeStats) entry.getValue();
			buf.append(name + " - " + ts.toString() + "\n");
		}
		System.out.println(buf.toString());
		return buf.toString();
	}

	static class TimeStats {
		long start;
		long end;
		int count = 0;
		long accum = 0;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;

		public TimeStats(long start) {
			this.start = start;
		}

		public void setEnd(long end) {
			long diff = end - start;
			accum += diff;
			count++;
			min = Math.min(diff, min);
			max = Math.max(diff, max);
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("(in msecs) - mean=").append(accum / (double) count);
			buf.append(", count=").append(count);
			buf.append(", min=").append(min);
			buf.append(", max=").append(max);
			return buf.toString();
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
