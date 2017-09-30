package edu.jhu.ir.documentsimilarity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.jhu.ir.documentsimilarity.InvertedFileAccessor.Term;


/**
 * Implementation of a simple retrieval engine based on cosine similarity using algebraic
 *    vector space models
 * Scores and ranks documents in order of their presumed relevance to queries
 * Builds an index and dictionary on disk, loads the dictionary and retrieves postings as
 *    needed from the inverted file to rank documents for given set of queries
 * Implements cosine scoring using TF-IDF term weighting for both documents and the queries
 * Computes the cosine similarity measure for documents in the collection containing at
 *    least one of the query terms
 *
 * Creates a second, separate index of the collection that uses different tokenization rules,
 *    specifically, truncates any term longer than 5 characters
 * Produces a second ranking of documents using this index
 *
 * @author Miranda Myers
 *
 */
public class DocumentSimilarity {

	private Map<Integer, Document> documents = new HashMap<>();	//Map of document ID to document object
	private Map<String, Term> lexicon = new HashMap<>();  // Lexicon that will hold all terms

	private String inputFileName;
	private InvertedFileAccessor invertedFileAccessor;

	public DocumentSimilarity(String inputFileName) {
		this.inputFileName = inputFileName;
		invertedFileAccessor = new InvertedFileAccessor(inputFileName);
		lexicon = invertedFileAccessor.getLexiconFromDisk();
	}

	private class Document {
		private float vectorLength;
		private Map<Integer, Float> scores; //Map of query ID to document score for that query
	}


	public void computeDocumentVectorLengths() {

	}

	/**
	 * Processes given file containing a set of queries
	 * Creates a bag of words representation for each query
	 *     B.O.W.: list of strings (terms) and weights (counts)
	 */
	public void processQuerySet() {

	}


	/**
	 * Prints the query vector for the first query in the given set of queries
	 * Displays the processed query terms and their weights for topic #76
	 */
	public void printFirstQuery() {

	}

	/**
	 * Produces a single output file containing ranked documents for all topics
	 * Provides the top 50 ranked documents for each query
	 */
	public void outputRankedDocuments() {

	}

	public void test() throws IOException {
		invertedFileAccessor.prettyPrintList(invertedFileAccessor.readInvertedIndex("sleep"));
	}

	public static void main(String[] args) throws IOException {
		DocumentSimilarity documentSimilarity = new DocumentSimilarity("fire10TEST.en.utf8");
		documentSimilarity.test();
	}
}
