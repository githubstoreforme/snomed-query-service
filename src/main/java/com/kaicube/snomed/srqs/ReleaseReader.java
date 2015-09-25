package com.kaicube.snomed.srqs;

import com.kaicube.snomed.srqs.domain.Concept;
import com.kaicube.snomed.srqs.exceptions.NotFoundException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReleaseReader {

	private final IndexSearcher indexSearcher;
	private final QueryParser parser;

	public ReleaseReader(ReleaseStore releaseStore) throws IOException {
		indexSearcher = new IndexSearcher(DirectoryReader.open(releaseStore.getDirectory()));
		final Analyzer analyzer = releaseStore.createAnalyzer();
		parser = new QueryParser(Version.LUCENE_40, Concept.ID, analyzer);
		parser.setAllowLeadingWildcard(true);
	}

	public long getConceptCount() throws IOException {
		return indexSearcher.collectionStatistics(Concept.ID).docCount();
	}

	public List<String> retrieveConcepts(int limit) throws ParseException, IOException {
		List<String> concepts = new ArrayList<>();
		final TopDocs topDocs = indexSearcher.search(parser.parse("*"), limit);
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			concepts.add(indexSearcher.doc(scoreDoc.doc).get(Concept.ID));
		}
		return concepts;
	}

	public String[] retrieveConcept(String conceptId) throws ParseException, IOException, NotFoundException {
		final Long idLong = new Long(conceptId);
		final TopDocs docs = indexSearcher.search(NumericRangeQuery.newLongRange(Concept.ID, idLong, idLong, true, true), 1);
//		final TopDocs docs = isearcher.search(parser.parse(conceptId), 1);
		if (docs.totalHits < 1) {
			throw new NotFoundException("Concept with id " + conceptId + " could not be found.");
		}
		final Document document = indexSearcher.doc(docs.scoreDocs[0].doc);
		return document.getValues(Concept.ANCESTOR);
	}
}