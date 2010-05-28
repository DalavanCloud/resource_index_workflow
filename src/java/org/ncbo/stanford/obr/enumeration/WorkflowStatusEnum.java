package org.ncbo.stanford.obr.enumeration;

public enum WorkflowStatusEnum {
	DIRECT_ANNOTATION_DONE(1),
	IS_A_CLOSURE_DONE(2),
	MAPPING_DONE(3),
	INDEXING_NOT_DONE(6),
	INDEXING_DONE(8)
	;
	private int status;

	private WorkflowStatusEnum(int status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	} 
	 
}
