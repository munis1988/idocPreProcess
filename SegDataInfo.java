package com.saki.idocpreprocess;

import java.util.ArrayLissakit;
import java.util.List;
import com.sap.conn.idoc.IDocSegmentMetaData;


public class SegDataInfo {

	List<SegDataInfo> segMetaDataList = new ArrayList<SegDataInfo>();	

String SegDef;
String SegType;

public SegDataInfo(){
	SegDef = "";
	SegType = "";
	
}


public String getSegDef() {
	return SegDef;
}
public void setSegDef(String segDef) {
	SegDef = segDef;
}
public String getSegType() {
	return SegType;
}
public void setSegType(String segType) {
	SegType = segType;
}

public void getMetaData(IDocSegmentMetaData currSeg, List<SegDataInfo> metaDataList){
	
	SegDataInfo currentSegment  = new SegDataInfo();
	currentSegment.setSegDef(currSeg.getDefinition());
	currentSegment.setSegType(currSeg.getType());
	metaDataList.add(currentSegment);
	
	String currDef = currSeg.getDefinition();
	String tempNum = currDef.substring(currDef.length()-3);
	 
	 
	 try {
		int lastNum = Integer.parseInt(tempNum);
		
		for ( int i = lastNum - 1; i >= 0 ; i--){
			String defnToReplace = currDef.substring(0,currDef.length()-3) + String.format("%03d", i);
			SegDataInfo newSegmentToAdd = new SegDataInfo();
			newSegmentToAdd.setSegDef(defnToReplace);
			newSegmentToAdd.setSegType(currSeg.getType());
			metaDataList.add(newSegmentToAdd);
		}
		
		SegDataInfo newSegmentToAdd = new SegDataInfo();
		String defnToReplace = currDef.substring(0,currDef.length()-3);
		newSegmentToAdd.setSegDef(defnToReplace);
		newSegmentToAdd.setSegType(currSeg.getType());
		metaDataList.add(newSegmentToAdd);
		
// Add one entry without the suffix digits		
		
	} catch (NumberFormatException e) {
	}
	 IDocSegmentMetaData[] children = currSeg.getChildren();
	 for(IDocSegmentMetaData currChild : children){
	 	getMetaData(currChild,metaDataList);
	 	
	 }
	 	
	 }


}