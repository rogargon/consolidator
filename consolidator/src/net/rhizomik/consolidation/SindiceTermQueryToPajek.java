package net.rhizomik.consolidation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.sindice.commons.helper.hbase.pagerepository.DefaultPageRepositorySchema;
import org.sindice.commons.helper.hbase.pagerepository.PageRepositoryClient;
import org.sindice.commons.helper.hbase.pagerepository.SindiceDocument;

public class SindiceTermQueryToPajek 
{
	static Logger log = Logger.getLogger("net.rhizomik.consolidation");
	private String query = "Galway";
	private PajekNetwork pajekNet = null;
	private HashSet<URL> processedSources = new HashSet<URL>();
	private PageRepositoryClient sindiceClient = null;
	static String sindice1KeywordQuery = "http://sindice.com/query/lookup?format=txt&keyword=";
	static String sindice1URIQuery = "http://sindice.com/query/lookup?format=txt&uri=";
	static int MAX_DEPTH = 1;
	
	public SindiceTermQueryToPajek (String termQuery) throws IOException 
	{
		this.query = URLEncoder.encode(termQuery, "UTF-8");
		this.pajekNet = new PajekNetwork(query);
		this.sindiceClient = PageRepositoryClient.getSindicePageRepository();
	}
	
	public void run() throws IOException
	{
		log.debug(">>>Processing query: "+query);
		for(URL source: SindiceSources.processTxtResult(sindice1KeywordQuery+query))
		{
			sindiceSourceToPajek(source);
			processedSources.add(source);
			expand(source, 0);
		}
		log.debug(processedSources.size()+" sources processed");
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
	
	private void sindiceSourceToPajek (URL source) throws IOException
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
				catch (ParseException e) { log.error(e+"\n"+triple); }
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
				catch (ParseException e) { log.error(e+"\n"+triple); }
			}
			log.debug("Processed "+triples+" implicit triples from "+source);
		}
		catch (Exception e) { log.error(e+" processing "+source); }
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
