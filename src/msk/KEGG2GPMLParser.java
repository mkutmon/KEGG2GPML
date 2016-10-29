package msk;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.bio.DataSourceTxt;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;

/**
 * Creates GPML files for all human KEGG pathways
 * only lists genes (Entrez Gene) and compounds (KEGG compounds)
 * no pathway layout is preserved
 * can be used for pathway statistics in PV but not for visualization
 * 
 * @author mkutmon
 *
 */
public class KEGG2GPMLParser {

	public static void main(String[] args) throws Exception {
		DataSourceTxt.init();

		// retrieves a list of human pathways (identifier + name)
		Map<String, String> pathwayNames = KEGG2GPMLParser.getPathwayName();

		// retrieves a link between human pathways and genes
		Map<String, Set<String>> pathwayGeneLink = KEGG2GPMLParser.getPathwayGeneLinks();
		
		// retrieves a link between pathways and compounds (some pathways might not exist in human 
		// and are filtered out based on pathwayNames map
		Map<String, Set<String>> pathwayCompoundLink = KEGG2GPMLParser.getPathwayCompoundLinks();
		
		// create directory in which new GPML files are created
		File outputDir = new File("output");
		outputDir.mkdir();
		
		// creates a GPML file for each human pathway
		for(String p : pathwayNames.keySet()) {
			Pathway pathway = new Pathway();
			pathway.getMappInfo().setMapInfoName(pathwayNames.get(p));
			pathway.getMappInfo().setMapInfoDataSource("KEGG: " + p);
			pathway.getMappInfo().setOrganism("Homo sapiens");
			
			// adds all genes in the pathway
			if(pathwayGeneLink.containsKey(p)) {
				int y = 70;
				for(String g : pathwayGeneLink.get(p)) {
					PathwayElement pe = PathwayElement.createPathwayElement(ObjectType.DATANODE);
					pe.setDataNodeType(DataNodeType.GENEPRODUCT);
					pe.setMCenterX(65);
					pe.setMCenterY(y);
					pe.setMHeight(20);
					pe.setMWidth(80);
					pe.setTextLabel(g);
					pe.setElementID(g);
					pe.setDataSource(DataSource.getExistingBySystemCode("L"));
					pathway.add(pe);
					y += 30;
				}
			}
			// adds all compounds in the pathway
			if(pathwayCompoundLink.containsKey(p)) {
				int y = 70;
				for(String c : pathwayCompoundLink.get(p)) {
					PathwayElement pe = PathwayElement.createPathwayElement(ObjectType.DATANODE);
					pe.setDataNodeType(DataNodeType.METABOLITE);
					pe.setColor(Color.BLUE);
					pe.setMCenterX(165);
					pe.setMCenterY(y);
					pe.setMHeight(20);
					pe.setMWidth(80);
					pe.setTextLabel(c);
					pe.setElementID(c);
					pe.setDataSource(DataSource.getExistingBySystemCode("Ck"));
					pathway.add(pe);
					y += 30;
				}
			}
			
			// writes the pathway into a GPML file
			pathway.writeToXml(new File(outputDir, p + ".gpml"), true);
		}
	}

	private static Map<String, Set<String>> getPathwayCompoundLinks() throws Exception {
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		URL urlPathways = new URL("http://rest.kegg.jp/link/pathway/compound");
		URLConnection con = urlPathways.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		while((line = reader.readLine()) != null) {
			String [] buffer = line.split("\t");
			String p = buffer[1].replace("path:", "");
			p = p.replace("map", "hsa");
			String c = buffer[0].replace("cpd:", "");
			if(!map.containsKey(p)) {
				map.put(p, new HashSet<String>());
			}
			map.get(p).add(c);
		}
		reader.close();
		return map;
	}

	private static Map<String, String> getPathwayName() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		URL urlPathways = new URL("http://rest.kegg.jp/list/pathway/hsa");
		URLConnection con = urlPathways.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		while((line = reader.readLine()) != null) {
			String [] buffer = line.split("\t");
			String p = buffer[0].replace("path:", "");
			String [] buffer2 = buffer[1].split(" - ");
			String n = buffer2[0];
			map.put(p, n);
		}
		reader.close();
		return map;
	}

	private static Map<String, Set<String>> getPathwayGeneLinks() throws Exception {
		Map<String, Set<String>> pathwayGeneLink = new HashMap<String, Set<String>>();
		URL urlPathways = new URL("http://rest.kegg.jp/link/hsa/pathway");
		URLConnection con = urlPathways.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		while((line = reader.readLine()) != null) {
			String [] buffer = line.split("\t");
			String p = buffer[0].replace("path:", "");
			String g = buffer[1].replace("hsa:", "");
			if(!pathwayGeneLink.containsKey(p)) {
				pathwayGeneLink.put(p, new HashSet<String>());
			}
			pathwayGeneLink.get(p).add(g);
		}
		
		reader.close();
		return pathwayGeneLink;
	}
}
