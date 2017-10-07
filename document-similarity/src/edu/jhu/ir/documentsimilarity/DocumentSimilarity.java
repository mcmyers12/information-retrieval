package edu.jhu.ir.documentsimilarity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.jhu.ir.documentsimilarity.IRUtil.InvertedFileRecord;
import edu.jhu.ir.documentsimilarity.IRUtil.Term;



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

	private Map<Integer, Double> documentVectorLengths = new HashMap<>();
	private Map<String, Term> lexicon = new HashMap<>();  // Lexicon that will hold all terms
	private Map<Integer, Query> querySet = new LinkedHashMap<>(); //Map of query id to query object that preserves original ordering of queries
	private boolean useStemming;
	private int numDocuments;
	private String queryFileName;
	private String outputFileName;
	private InvertedFileAccessor invertedFileAccessor;
	private double cosineSimilarityRuntime;

	public DocumentSimilarity(String inputFileName, String queryFileName, String outputFileName, boolean useStemming) {
		this.outputFileName = outputFileName;
		this.queryFileName = queryFileName;
		this.useStemming = useStemming;
		invertedFileAccessor = new InvertedFileAccessor(inputFileName, useStemming);
		lexicon = invertedFileAccessor.getLexiconFromDisk();
		numDocuments = invertedFileAccessor.getNumDocuments();
	}


	private class Query {
		private int id;
		private Map<String, Integer> bagOfWords = new HashMap<>();
		private Map<Integer, Double> documentScores = new LinkedHashMap<>(); //Map of document ID to score for the query, preserves insertion order

		public Query(int id) {
			this.id = id;
		}
	}


	//Document frequency is stored in the dictionary	//TODO possibly precompute this???
	private double getIdf(String term) {
		int documentFrequency = 0;
		if (lexicon.containsKey(term)) {
			documentFrequency = lexicon.get(term).getDocumentFrequency();
		}

		double idf;
		if (documentFrequency > 0) {
			idf = Math.log(numDocuments / documentFrequency) / Math.log(2);	//Log base 2
		}
		else {
			idf = 0;
		}

		return idf;
	}


	//Square root of sum of squares of all of the term weights in the query
	private double computeQueryVectorLength(Query query) {
		double queryVectorLength = 0;
		for (Map.Entry<String, Integer> entry : query.bagOfWords.entrySet()) {
			int queryTermFrequency = entry.getValue();
			String term = entry.getKey();
			double tfIdf = queryTermFrequency * getIdf(term);

			queryVectorLength += tfIdf * tfIdf;
		}
		queryVectorLength = Math.sqrt(queryVectorLength);

		return queryVectorLength;
	}


	//We return the documents ranked by the closeness of their vectors to the query

	//Put each documentID into a hash table pointing to scores - key: docId, value: score
	//Scores should be initialized to 0

	//Keep an accumulator of docId -> scores
	//Cosine scores are computed one term at a time
	//	Take the first query term, seek into the inverted file to find docIds and term counts
	//		compute the tf-idf weight, multiply that by the appropriate query term tf-idf,
	//		add that product (partial dot product) into an accumulator where we are storing the document scores
	//		As we process more and more terms, the scores for documents will tend to increase
	//	Only consider terms both in document and query
	//After all query terms are processed, divide partial dot product by query length * document length
	//Then, sort the documents by score - heapsort may be best

	//Last part of implementation video discusses options to improve efficiency
	private void computeQueryScores(Query query) throws IOException {

		//For the dot product part of cosine metric, we can use only the terms that are in the query
		Map<Integer, Double> scoreAccumulator = new HashMap<>(); //Document ID to dot product

		double queryVectorLength = computeQueryVectorLength(query);

		for (String term : query.bagOfWords.keySet()) {

			double queryTfIdf = query.bagOfWords.get(term) * getIdf(term);

			//Get the files that have the query term
			List<InvertedFileRecord> invertedFileRecords = invertedFileAccessor.readInvertedIndex(term);

			for (InvertedFileRecord invertedFileRecord : invertedFileRecords) {
				int documentId = invertedFileRecord.getDocumentId();
				double documentTfIdf = invertedFileRecord.getTermFrequency() * getIdf(term);
				double partialDotProduct = queryTfIdf * documentTfIdf;

				if (scoreAccumulator.containsKey(documentId)) {
					double dotProductAccumulator = scoreAccumulator.get(documentId);
					partialDotProduct += dotProductAccumulator;
				}

				scoreAccumulator.put(documentId, partialDotProduct);
			}
		}

		for (int documentId : scoreAccumulator.keySet()) {
			double dotProduct = scoreAccumulator.get(documentId);
			double documentVectorLength = documentVectorLengths.get(documentId);
			double denominator = documentVectorLength * queryVectorLength;

			double cosineScore;
			if (denominator == 0) {
				cosineScore = 0;
			}
			else {
				cosineScore = dotProduct / (documentVectorLength * queryVectorLength);
			}

			scoreAccumulator.put(documentId, cosineScore);
		}

		query.documentScores = scoreAccumulator;
	}


	//Term frequency is stored in the inverted file
	//Query vector length can be computed one time and reused to rank the different documents
	//To compute document length, you need to compute the TF-IDF values for each term in the document
	//DF is not know for all terms until the last term is seen
	//
	//Sum of squares of all of the term weights in the whole document
	//After all documents have been seen, doc lengths can be computed one term at a time,
	//	by walking over every entry in the dictionary, look up entry in the inverted file
	//	calculate TF-IDF, store that squared value stored into an accumulator for each doc ID
	//A sum of squared weights will then be stored for each document, and after all terms are seen,
	//	take sqrt
	private void computeDocumentVectorLengths() throws IOException {
		Map<Integer, Double> documentTfIdfAccumulator = new HashMap<>();
		for (String term : lexicon.keySet()) { //Get squared tf-idf weights for all terms in all documents
			List<InvertedFileRecord> invertedFileRecords = invertedFileAccessor.readInvertedIndex(term);
			for (InvertedFileRecord invertedFileRecord : invertedFileRecords) {
				int documentId = invertedFileRecord.getDocumentId();
				int tf = invertedFileRecord.getTermFrequency();
				double idf = getIdf(term);
				double tfIdf = tf * idf;

				double squaredWeights;
				if (documentTfIdfAccumulator.containsKey(documentId)) { //If this document already has squared weights, add to it
					squaredWeights = documentTfIdfAccumulator.get(documentId);
					squaredWeights += tfIdf * tfIdf;
				}
				else { //Otherwise initialize the entry in the map with a new squared weight
					squaredWeights = tfIdf * tfIdf;
				}

				documentTfIdfAccumulator.put(documentId, squaredWeights);
			}
		}

		for (int documentId : documentTfIdfAccumulator.keySet()) {
			double squaredWeights = documentTfIdfAccumulator.get(documentId);
			squaredWeights = Math.sqrt(squaredWeights);
			documentVectorLengths.put(documentId, squaredWeights);
		}
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
				querySet.put(queryId, new Query(queryId));

				currentLine = bufferedReader.readLine();
				Map<String, Integer> tokensInQuery = new HashMap<>();

				while (currentLine != null && !currentLine.startsWith("</Q>")) {
					List<String> tokens = IRUtil.tokenize(currentLine);

					for (String token : tokens) {
						if (useStemming) {
							if (token.length() > 5) {
								token = token.substring(0, 5);
							}
						}

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
	}


	/**
	 * Produces a single output file containing ranked documents for all topics
	 * Provides the top 50 ranked documents for each query
	 * @throws IOException
	 */
	private void computeAllScores() throws IOException {
		processQueryFile();
		computeDocumentVectorLengths();
		for (Query query : querySet.values()) { //TODO make sure this is processed in query order
			computeQueryScores(query);
			Map<Integer, Double> sortedScores = IRUtil.sortMapByValue(query.documentScores);
			query.documentScores = sortedScores;
		}
	}


	public void outputRankedDocuments() throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(outputFileName);
		for (Query query : querySet.values()) {
			int rank = 1;
			for (Map.Entry<Integer, Double> score : query.documentScores.entrySet()) {
				writer.print(query.id + " Q0 " + score.getKey() + " " + rank + " ");
				writer.printf("%.9f", score.getValue());		//TODO how many decimal places???
				writer.println(" myers");
				if (rank == 50) {
					break;
				}
				rank++;
			}
		}
		writer.close();
	}


	/**
	 * Prints the query vector for the first query in the given set of queries
	 * Displays the processed query terms and their weights for topic #76
	 */
	private void printRunStatistics() {
		System.out.println("-------Program output for cosine similarity scoring " + (useStemming ? "with stemming-------\n" : "without stemming-------\n"));
		Query query = querySet.get(1);									//TODO CHANGE THIS BACK TO 76
		System.out.println("Terms and weights (counts) for query #76:");
		for (Map.Entry<String, Integer> termWeights : query.bagOfWords.entrySet()) {
			System.out.println("\t" + termWeights.getKey() + ": " + termWeights.getValue());
		}
		System.out.println();

		System.out.println("Vocabulary size: " + invertedFileAccessor.getVocabularySize() + "\n");
		System.out.println("Number of documents indexed: " + numDocuments + "\n");

		if (useStemming) {
			System.out.println("Run-time for cosine similarity using stemming: " + cosineSimilarityRuntime + " minutes\n");
		}
		else {
			System.out.println("Run-time for cosine similarity without stemming: " + cosineSimilarityRuntime + " minutes\n");
		}

		invertedFileAccessor.printFileSizeInformation();
		System.out.println("\n---------------------------------------------------------------------------\n\n");
	}


	public void computeDocumentSimilarity() throws IOException {
		long startTime = System.currentTimeMillis();
		computeAllScores();
		outputRankedDocuments();
		long endTime = System.currentTimeMillis();
		cosineSimilarityRuntime = endTime - startTime;
		cosineSimilarityRuntime = cosineSimilarityRuntime / 1000 / 60;

		printRunStatistics();
	}


	public static void main(String[] args) throws IOException {
		DocumentSimilarity documentSimilarityWithoutStemming = new DocumentSimilarity("animal.txt", "animal.topics.txt", "myers-a.txt", false);
		documentSimilarityWithoutStemming.computeDocumentSimilarity();

		DocumentSimilarity documentSimilarityWithStemming = new DocumentSimilarity("animal.txt", "animal.topics.txt", "animal-output.txt", true);
		documentSimilarityWithStemming.computeDocumentSimilarity();
	}
}
