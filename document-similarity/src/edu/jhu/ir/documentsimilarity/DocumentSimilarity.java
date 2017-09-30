package edu.jhu.ir.documentsimilarity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
	private Map<Integer, Query> querySet = new HashMap<>();

	private String inputFileName;
	private String queryFileName;
	private InvertedFileAccessor invertedFileAccessor;

	public DocumentSimilarity(String inputFileName, String queryFileName) {
		this.inputFileName = inputFileName;
		this.queryFileName = queryFileName;
		invertedFileAccessor = new InvertedFileAccessor(inputFileName);
		lexicon = invertedFileAccessor.getLexiconFromDisk();
	}

	private class Document {
		private float vectorLength;
		private Map<Integer, Float> scores; //Map of query ID to document score for that query
	}

	private class Query {
		private Map<String, Integer> bagOfWords = new HashMap<>();
	}


	public void computeDocumentVectorLengths() {

	}


	/**
	 * Processes given file containing a set of queries
	 * Creates a bag of words representation for each query
	 *     B.O.W.: list of strings (terms) and weights (counts)
	 */
	public void processQueryFile() {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(queryFileName);
			bufferedReader = new BufferedReader(fileReader);

			buildQuerySet(bufferedReader);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void buildQuerySet(BufferedReader bufferedReader) throws IOException {
		String currentLine;

		while ((currentLine = bufferedReader.readLine()) != null) {
			if (currentLine.startsWith("<Q ID=")) { // The start of a new query
				int queryId = Integer.parseInt(currentLine.replace("<Q ID=", "").replace(">", ""));
				querySet.put(queryId, new Query());

				currentLine = bufferedReader.readLine();
				Map<String, Integer> tokensInQuery = new HashMap<>();

				while (currentLine != null && !currentLine.startsWith("</Q>")) {
					List<String> tokens = ParsingUtil.tokenize(currentLine);

					for (String token : tokens) {
						if (tokensInQuery.containsKey(token)) {
							int count = tokensInQuery.get(token).intValue();
							tokensInQuery.put(token, ++count);
						}
						else {
							tokensInQuery.put(token, 1);
						}
					}

					currentLine = bufferedReader.readLine();
				}
				querySet.get(queryId).bagOfWords = tokensInQuery;
			}
		}

		for (Map.Entry<Integer, Query> entry : querySet.entrySet()) {
			System.out.println(entry.getKey());
			for (Map.Entry<String, Integer> bowEntry : entry.getValue().bagOfWords.entrySet()) {
				System.out.println("\t" + bowEntry.getKey() + " " + bowEntry.getValue());
			}
			System.out.println();
		}
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
		processQueryFile();
	}

	public static void main(String[] args) throws IOException {
		DocumentSimilarity documentSimilarity = new DocumentSimilarity("fire10TEST.en.utf8", "fire10.topics.en.utf8");
		documentSimilarity.test();
	}
}
