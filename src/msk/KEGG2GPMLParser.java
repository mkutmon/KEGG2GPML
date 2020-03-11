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

	public Map<String, Set<String>> pathwayGeneLink;
	public Map<String, Set<String>> pathwayCompoundLink;
	public Map<String, String> pathwayNames;
	
	public KEGG2GPMLParser() {
		pathwayGeneLink = new HashMap<String, Set<String>>();
		pathwayCompoundLink = new HashMap<String, Set<String>>();
		pathwayNames = new HashMap<String, String>();
	}
	
	public void createGPMLFiles(String species, File outputDir) throws Exception {
		DataSourceTxt.init();
		
		// retrieves a list of pathways (identifier + name)
		System.out.println("Retrieve pathway list.");
		getPathwayName(species);
		System.out.println(pathwayNames.size() + " pathways found for " + getSpeciesName(species) + ".");

		// retrieves a link between human pathways and genes
		System.out.println("Retrieve gene lists.");
		getPathwayGeneLinks(species);
		
		// retrieves a link between pathways and compounds (some pathways might not exist in human 
		// and are filtered out based on pathwayNames map
		System.out.println("Retrieve compound lists.");
		getPathwayCompoundLinks(species);
		
		// creates a GPML file for each human pathway
		System.out.println("Save pathways as GPML.");
		for(String p : pathwayNames.keySet()) {
			Pathway pathway = createPathway(p, species);
			pathway.writeToXml(new File(outputDir, p + ".gpml"), true);
		}
		
		System.out.println(pathwayNames.size() + " pathways converted to GPML and saved in " + outputDir.getAbsolutePath());
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length == 2) {
			String species = args[0];
			String outputFile = args[1];
			// create directory in which new GPML files are created
			File outputDir = new File(outputFile);
			outputDir.mkdir();
			
			KEGG2GPMLParser parser = new KEGG2GPMLParser();
			parser.createGPMLFiles(species, outputDir);
		} else {
			System.err.println("Please provide species (e.g. hsa for human) and output directory.");
		}
	}
	
	private String getSpeciesName(String species) throws Exception {
		URL urlPathways = new URL("http://rest.kegg.jp/info/" + species);
		URLConnection con = urlPathways.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line = reader.readLine();
		String [] buffer = line.split("           ");
		String [] buffer2 = buffer[1].split("\\(");
		return buffer2[0].substring(0, buffer2[0].length()-1);
	}
	
	private Pathway createPathway(String p, String species) throws Exception {
		Pathway pathway = new Pathway();
		pathway.getMappInfo().setMapInfoName(pathwayNames.get(p));
		pathway.getMappInfo().setMapInfoDataSource("KEGG: " + p);
		pathway.getMappInfo().setOrganism(getSpeciesName(species));
		
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
		
		return pathway;
	}

	private void getPathwayCompoundLinks(String species) throws Exception {
		URL urlPathways = new URL("http://rest.kegg.jp/link/pathway/compound");
		URLConnection con = urlPathways.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		while((line = reader.readLine()) != null) {
			String [] buffer = line.split("\t");
			String p = buffer[1].replace("path:", "");
			p = p.replace("map", species);
			String c = buffer[0].replace("cpd:", "");
			if(!pathwayCompoundLink.containsKey(p)) {
				pathwayCompoundLink.put(p, new HashSet<String>());
			}
			pathwayCompoundLink.get(p).add(c);
		}
		reader.close();
	}

	private void getPathwayName(String species) throws Exception {
		URL urlPathways = new URL("http://rest.kegg.jp/list/pathway/" + species);
		URLConnection con = urlPathways.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		while((line = reader.readLine()) != null) {
			String [] buffer = line.split("\t");
			String p = buffer[0].replace("path:", "");
			String [] buffer2 = buffer[1].split(" - ");
			String n = buffer2[0];
			pathwayNames.put(p, n);
		}
		reader.close();
	}

	private void getPathwayGeneLinks(String species) throws Exception {
		URL urlPathways = new URL("http://rest.kegg.jp/link/" + species + "/pathway");
		URLConnection con = urlPathways.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		while((line = reader.readLine()) != null) {
			String [] buffer = line.split("\t");
			String p = buffer[0].replace("path:", "");
			String g = buffer[1].replace(species + ":", "");
			if(!pathwayGeneLink.containsKey(p)) {
				pathwayGeneLink.put(p, new HashSet<String>());
			}
			pathwayGeneLink.get(p).add(g);
		}
		
		reader.close();
	}
}
