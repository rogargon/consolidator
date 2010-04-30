package net.rhizomik.consolidation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.sun.corba.se.impl.copyobject.JavaStreamObjectCopierImpl;

public class SindiceTermQueryToPajek 
{
	static Logger log = Logger.getLogger("net.rhizomik.consolidation");
	private String query = "Galway";
	private PajekNetwork pajekNet = null;
	private HashSet<URL> processedSources = new HashSet<URL>();
	
	static String sindice1KeywordQuery = "http://sindice.com/query/lookup?format=txt&keyword=";
	static String sindice1Query = "http://sindice.com/query/lookup?format=txt&";
	static String sindice1URIQuery = "http://sindice.com/query/lookup?format=txt&uri=";
	static String sindiceCache = "http://api.sindice.com/v2/cache?url=";
	
	static int MAX_DEPTH = 0;
	
	public SindiceTermQueryToPajek (String termQuery) throws IOException 
	{
		this.query = termQuery; //URLEncoder.encode(termQuery, "UTF-8");
		this.pajekNet = new PajekNetwork(query);
	}
	
	public void run() throws IOException
	{
		log.log(Level.INFO, "Processing query: "+query);
		for(URL source: SindiceSources.processTxtResult(sindice1Query+query))
		{
			sindiceSourceToPajek(source);
			processedSources.add(source);
			expand(source, 0);
		}
		log.log(Level.INFO, processedSources.size()+" sources processed");
	}
	
	private void expand(URL uri, int depth) throws IOException
	{
		if (depth < MAX_DEPTH)
		{
			for(URL source: SindiceSources.processTxtResult(sindice1URIQuery+uri))
			{
				if (!processedSources.contains(source))
				{
					sindiceSourceToPajek(source);
					processedSources.add(source);
					expand(source, depth+1);
				}
			}
		}
	}
	
	private void sindiceSourceToPajekDirect (URL source) throws IOException
	{
		try
		{
			SindiceDocument document = sindiceClient.getDocument(source.toString());
		
			int triples = 0;
			for (String triple : document.get(DefaultPageRepositorySchema.FIELD_EXPLICIT_CONTENT))
			{
				try 
				{ 
					pajekNet.addArc(triple, source.toString(), false);
					triples++;
				}
				catch (ParseException e) { log.log(Level.SEVERE, e+"\n"+triple); }
			}
			log.debug("Processed "+triples+" explicit triples from "+source);
			triples = 0;
			for (String triple : document.get(DefaultPageRepositorySchema.FIELD_IMPLICIT_CONTENT))
			{
				try 
				{ 
					pajekNet.addArc(triple, source.toString(), true);
					triples++;
				}
				catch (ParseException e) { log.log(Level.SEVERE, e+"\n"+triple); }
			}
			log.log(Level.INFO, "Processed "+triples+" implicit triples from "+source);
		}
		catch (Exception e) { log.log(Level.SEVERE, e+" processing "+source); }
	}
	
	private void sindiceSourceToPajek (URL source) throws IOException
	{
		Model sModel = sindiceSourceToJena(source);
		File fRules = new File("coauthorship.jrule");
		Model iModel = filterModel(sModel, fRules);
		jenaToPajek(iModel, source);
	}
	
	private Model sindiceSourceToJena (URL source) throws IOException
	{
		Model m = ModelFactory.createDefaultModel();
		
		try
		{
			
			SindiceDocument document = sindiceClient.getDocument(source.toString());

			for (String triple : document.get(DefaultPageRepositorySchema.FIELD_EXPLICIT_CONTENT))
				m.read(new StringReader(triple), source.toString(), "N-TRIPLE");
			for (String triple : document.get(DefaultPageRepositorySchema.FIELD_IMPLICIT_CONTENT))
				m.read(new StringReader(triple), source.toString(), "N-TRIPLE");
		}
		catch (Exception e) { log.log(Level.SEVERE, e+" processing "+source); }
		
		return m;
	}
	
	private Model filterModel(Model sModel, File fRules) throws FileNotFoundException
	{
		BufferedReader brfRules = new BufferedReader(new FileReader(fRules));
		Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(Rule.rulesParserFromReader(brfRules)));
		InfModel inf = ModelFactory.createInfModel(reasoner, sModel);
		return inf.getDeductionsModel();
	}
	
	private void jenaToPajek(Model m, URL source) throws IOException
	{
		StringWriter ntriples = new StringWriter();
		m.write(ntriples, "N-TRIPLE");
		BufferedReader brNtriples = new BufferedReader(new StringReader(ntriples.toString()));
		int triples = 0;
		String triple = "";
		while ((triple = brNtriples.readLine()) != null)
		{
			try 
			{ 
				pajekNet.addArc(triple, source.toString(), true);
				triples++;
			}
			catch (ParseException e) { log.log(Level.SEVERE, e+"\n"+triple); }
		}
		log.log(Level.INFO, "Processed "+triples+" filtered triples from "+source);
	}
	
	public File serialisePajekNet() throws IOException
	{
		File f = new File(query+".d"+MAX_DEPTH+"s"+SindiceSources.RESULTS_PAGES+".net");
		pajekNet.serialisePajek(new FileWriter(f));
		return f;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 0) {
			System.err
					.println("Please, specify a terms query. For instance: Michael Jackson");
			return;
		}
		
		String queryTerms = "";
		for (String arg: args)
			queryTerms+=arg+" ";
		
		SindiceTermQueryToPajek query = new SindiceTermQueryToPajek(queryTerms.trim());
		query.run();
		File f = query.serialisePajekNet();

		System.out.println("Network written to "+f.getCanonicalPath());
	}
}
