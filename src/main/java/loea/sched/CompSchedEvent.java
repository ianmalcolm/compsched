package loea.sched;

import org.cloudbus.cloudsim.core.SimEvent;

public class CompSchedEvent {

	private final int etype;
	private final double time;
	private final int entSrc;
	private final int entDst;
	private final CompSchedTags tag;
	private final Object data;

	public int getEtype() {
		return etype;
	}

	public double getTime() {
		return time;
	}

	public int getEntSrc() {
		return entSrc;
	}

	public int getEntDst() {
		return entDst;
	}

	public CompSchedTags getTag() {
		return tag;
	}

	public Object getData() {
		return data;
	}

	public CompSchedEvent(SimEvent ev) {
		etype = ev.getType();
		time = ev.eventTime();
		entSrc = ev.getSource();
		entDst = ev.getDestination();

		tag = ((TagDataPair) ev.getData()).getTag();
		data = ((TagDataPair) ev.getData()).getData();
	}

	public static Object createTagDataPair(CompSchedTags _tags, Object _data) {
		TagDataPair pair = new TagDataPair(_tags, _data);
		return (Object) pair;
	}
}

class TagDataPair {

	CompSchedTags tag;
	Object data;

	public TagDataPair(CompSchedTags _tag, Object _data) {
		tag = _tag;
		data = _data;
	}

	CompSchedTags getTag() {
		return tag;
	}

	void setTag(CompSchedTags tag) {
		this.tag = tag;
	}

	Object getData() {
		return data;
	}

	void setData(Object data) {
		this.data = data;
	}

}
