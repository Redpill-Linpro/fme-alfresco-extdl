package se.redpill.alfresco.repo.jscript;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.apache.commons.lang.StringUtils;
import org.mozilla.javascript.Scriptable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class DatalistJsHelper extends BaseScopableProcessorExtension {

	
	/**
	 * When using the association picker in the datalist filtering
	 * this method is called to construct a netto-list of associations to use
	 * for filtering. 
	 * 
	 * @param json
	 * @return json-string with the netto list of associations
	 */
	public String mergeAssociations(String json){
				
		// convert into java objects
		Gson gson = new Gson();
		List<NameValueBean> assocs = null;
		Type collectionType = new TypeToken<List<NameValueBean>>() {
		}.getType();
		if (json != null) {
			assocs = gson.fromJson(json,
					collectionType);
		}
		
		Map<String, Set<String>> added = new HashMap<>();
		
		Map<String, Set<String>> removed = new HashMap<>();
		
		for(NameValueBean nvb : assocs){
			String suffix = StringUtils.substringAfterLast(nvb.getName(), "_");
			String assocName = StringUtils.substringBeforeLast(nvb.getName(), "_");
			if ("removed".equals(suffix)){
				Set<String> nodes = null;
				if(removed.containsKey(assocName)){
					nodes = removed.get(assocName);
				}else {
					nodes = new HashSet<>();
				}
				
				StringTokenizer st = new StringTokenizer(nvb.getValue(),",");
				while (st.hasMoreTokens()){
					nodes.add(st.nextToken());
				}
				
				removed.put(assocName, nodes);
				
			}else if ("added".equals(suffix)||"orig".equals(suffix)){
				Set<String> nodes = null;
				
				if(added.containsKey(assocName)){
					nodes = added.get(assocName);
				} else {
					nodes = new HashSet<>();
				}
				StringTokenizer st = new StringTokenizer(nvb.getValue(), ",");
				while (st.hasMoreTokens()){
					nodes.add(st.nextToken());
				}
				added.put(assocName, nodes);
				
			}
		}
		// Now subtract the values in the removed map from the values added map for each key
		Set<String> keySet = added.keySet();

		List<String> keysToRemove = new ArrayList<String>();
		for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext();) {
			String addedKey = (String) iterator.next();
			Set<String> addedValues = added.get(addedKey);
			Set<String> removedValues = removed.get(addedKey);
			if (removedValues != null){
				addedValues.removeAll(removedValues);	
			}
			// If we have no value left, remove the key as well.
			if (addedValues.isEmpty()){
				iterator.remove();
				keysToRemove.add(addedKey);
			}
		}
		
		// We dont want a map with empty values (Sets) 
		for (String key : keysToRemove) {
			added.remove(key);
		}
		
		String result= null;
		
		if (added != null && added.size() > 0){
			result = gson.toJson(added);
		}
	
		return result;
	}


}
