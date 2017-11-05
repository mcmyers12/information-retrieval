package edu.jhu.ir.webqueryloganalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Utility methods to help answer the questions about a dataset of user queries
 *
 * The dataset is a TSV file containing timestamp, user ID, first rank, and query string
 *
 * The questions answered about the dataset are as follows:
 * 		What is the average number of queries per user id?
 *		Report the mean and median query length in both words and characters.
 *		What percentage of queries are mixed case? All upper case? All lower case?
 *		What percent of the time does a user request only the top 10 results?
 *		What are the 20-most common queries issued?
 *		What percentage of queries were asked by only one user?
 *		Which occurs in queries more often "Al Gore" or "Johns Hopkins"? "Johns Hopkins" or "John Hopkins"?
 *		How often do URLs appear in queries?
 *		What are the 20 most common non-stopwords appearing in queries?
 *		What percent of queries contain stopwords like ‘and’, ‘the’, ‘of’, ‘in’, ‘at’?
 *
 *		What are the most popular websites mentioned in the queries?
 *		How does query volume change throughout the day?
 * 		How often is search engine “query” syntax used, like phrases in quotes, Boolean operators, or ‘+’ or ‘-‘ signs?
 *
 * @author Miranda Myers
 *
 */
public class WebQueryLogAnalysis {

	private List<List<String>> data = new ArrayList<>();
	private int numQueries;


	public void readTsvFile() throws IOException {
		BufferedReader tsvFile = new BufferedReader(new FileReader("19991220-Excite-QueryLog.utf8.tsv"));
		String row = tsvFile.readLine();

		while (row != null) {
			String[] columns = row.split("\t", -1);
			List<String> rowData = new ArrayList<>();
			for (int i = 0; i <= 3; i++) {
				rowData.add(columns[i]);
			}

			data.add(rowData);

			row = tsvFile.readLine();
		}

		tsvFile.close();
		numQueries = data.size();
	}


	public void queryVolume() {
		Map<String, Integer> hourCounts = new LinkedHashMap<>();

		for (List<String> row : data) {
			String hour = row.get(0).trim().substring(0, 2);

			if (hourCounts.containsKey(hour)) {
				int count = hourCounts.get(hour);
				hourCounts.put(hour, ++count);
			}
			else {
				hourCounts.put(hour, 1);
			}
		}

		System.out.println("How does query volume change throughout the day?\n\thour\tnum queries");

		for (Map.Entry<String, Integer> entry: hourCounts.entrySet()) {
			System.out.println("\t" + entry.getKey() + "\t" + entry.getValue());
		}
		System.out.println("\n");
	}


	public void frequencyOfQuerySyntax() {
		int querySyntaxCount = 0;

		for (List<String> row : data) {
			String query = row.get(3).trim();
			if (IRUtil.containsQuerySyntax(query)) {
				querySyntaxCount++;
			}
		}

		double percentageQuerySyntax = ((double) querySyntaxCount / numQueries) * 100;
		percentageQuerySyntax = Math.round(percentageQuerySyntax * 100.0) / 100.0;

		System.out.println("How often is search engine “query” syntax used, like phrases in quotes, Boolean operators, or ‘+’ or ‘-‘ signs?\n\t" + "Query syntax is used " + percentageQuerySyntax + "% of the time\n\n");
	}

	public void averageQueriesPerUserId() {
		Set<String> userIds = new HashSet<>();
		for (List<String> row : data) {
			String userId = row.get(1);
			userIds.add(userId);
		}

		int numUserIds = userIds.size();

		int averageQueriesPerUserId = numQueries / numUserIds;

		System.out.println("What is the average number of queries per user id?\n\t" + averageQueriesPerUserId + "\n\n");
	}

	public void percentageOfTopTenRequests() {
		int numTopTenRequests = 0;

		for (List<String> row : data) {
			String request = row.get(2).trim();
			if (request.equals("0")) {
				numTopTenRequests++;
			}
		}

		double percentageTopTenRequests = ((double) numTopTenRequests / numQueries) * 100;
		percentageTopTenRequests = Math.round(percentageTopTenRequests * 100.0) / 100.0;

		System.out.println("What percent of the time does a user request only the top 10 results?\n\t" + percentageTopTenRequests + "%\n\n");
	}


	public void percentageAskedByOneUser() {
		Map<String, Integer> queryCounts = new HashMap<>();

		for (List<String> row : data) {
			String query = row.get(3).trim();

			if (queryCounts.containsKey(query)) {
				int count = queryCounts.get(query);
				queryCounts.put(query, ++count);
			}
			else {
				queryCounts.put(query, 1);
			}
		}

		Map<String, Integer> sortedQueryCounts = IRUtil.sortMapByValueAscending(queryCounts);

		int numOneUserQueries = 0;
		Iterator<Entry<String, Integer>> it = sortedQueryCounts.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, Integer> entry = it.next();
			int count = entry.getValue();
			if (count == 1) {
				numOneUserQueries++;
			}
			else {
				break;
			}
		}

		double percentageNumOneUserQueries = ((double) numOneUserQueries / numQueries) * 100;
		percentageNumOneUserQueries = Math.round(percentageNumOneUserQueries * 100.0) / 100.0;

		System.out.println("What percentage of queries were asked by only one user?\n\t" + percentageNumOneUserQueries + "%\n\n");
	}


	public void mostCommonQueries() {
		System.out.println("What are the 20-most common queries issued?");

		Map<String, Integer> queryCounts = new HashMap<>();
		for (List<String> row : data) {
			String query = row.get(3).trim();

			if (queryCounts.containsKey(query)) {
				int count = queryCounts.get(query);
				queryCounts.put(query, ++count);
			}
			else {
				queryCounts.put(query, 1);
			}
		}

		Map<String, Integer> sortedQueryCounts = IRUtil.sortMapByValueDescending(queryCounts);
		Iterator<Entry<String, Integer>> it = sortedQueryCounts.entrySet().iterator();
		int count = 1;
		while(it.hasNext() && count <= 20) {
			String query = it.next().getKey();
			System.out.println("\t" + count + ". " + query + " (" + sortedQueryCounts.get(query) + " times)");
			count++;
		}
		System.out.println("\n");
	}


	public void mostCommonNonStopwords() {
		System.out.println("What are the 20 most common non-stopwords appearing in queries?");

		Map<String, Integer> nonStopwordCounts = new HashMap<>();
		for (List<String> row : data) {
			String query = row.get(3).trim();

			for (String token : IRUtil.tokenize(query)) {
				if (!IRUtil.isStopword(token)) {
					if (nonStopwordCounts.containsKey(token)) {
						int count = nonStopwordCounts.get(token);
						nonStopwordCounts.put(token, ++count);
					}
					else {
						nonStopwordCounts.put(token, 1);
					}
				}
			}
		}

		Map<String, Integer> sortedNonStopwordCounts = IRUtil.sortMapByValueDescending(nonStopwordCounts);
		Iterator<Entry<String, Integer>> it = sortedNonStopwordCounts.entrySet().iterator();
		int count = 1;
		while(it.hasNext() && count <= 20) {
			String term = it.next().getKey();
			System.out.println("\t" + count + ". " + term + " (" + sortedNonStopwordCounts.get(term) + " times)");
			count++;
		}
		System.out.println("\n");
	}


	public void queriesWithStopwords() {
		int queriesWithStopwords = 0;
		for (List<String> row : data) {
			String query = row.get(3).trim();
			if (IRUtil.queryContainsStopword(query)) {
				queriesWithStopwords++;
			}
		}

		double percentageQueriesWithStopwords = ((double) queriesWithStopwords / numQueries) * 100;
		percentageQueriesWithStopwords = Math.round(percentageQueriesWithStopwords * 100.0) / 100.0;
		System.out.println("What percent of queries contain stopwords like ‘and’, ‘the’, ‘of’, ‘in’, ‘at’?\n\t" + percentageQueriesWithStopwords + "%\n\n");
	}


	public void mostCommonPhrasesInQueries() {
		Map<String, Integer> termCounts = new HashMap<>();
		termCounts.put("al gore", 0);
		termCounts.put("johns hopkins", 0);
		termCounts.put("johns hopkins?", 0);
		termCounts.put("john hopkins", 0);

		for (List<String> row : data) {
			String query = row.get(3).trim();
			List<String> tokens = IRUtil.tokenize(query);

			String twoTermToken = "";
			for (int i = 0; i < tokens.size() - 1; i++) {
				twoTermToken = tokens.get(i) + " " + tokens.get(i + 1);

				if (twoTermToken.equals("al gore") || twoTermToken.equals("johns hopkins") || twoTermToken.equals("johns hopkins?") || twoTermToken.equals("john hopkins")) {
					int count = termCounts.get(twoTermToken);
					termCounts.put(twoTermToken, ++count);
				}
			}
		}

		Map<String, Integer> sortedTermCounts = IRUtil.sortMapByValueDescending(termCounts);
		String mostCommon = sortedTermCounts.entrySet().iterator().next().getKey();

		System.out.println("Which occurs in queries more often \"Al Gore\" or \"Johns Hopkins?\" \"Johns Hopkins\" or \"John Hopkins\"?\n\t" + mostCommon + "\n\n");
	}


	public void mostCommonWebsites() {
		System.out.println("What are the most popular websites mentioned in the queries?");

		Map<String, Integer> websiteCounts = new HashMap<>();
		for (List<String> row : data) {
			String query = row.get(3).trim();

			for (String token : IRUtil.tokenize(query)) {
				if (IRUtil.isWebsite(token)) {
					if (websiteCounts.containsKey(token)) {
						int count = websiteCounts.get(token);
						websiteCounts.put(token, ++count);
					}
					else {
						websiteCounts.put(token, 1);
					}
				}
			}
		}

		Map<String, Integer> sortedWebsiteCounts = IRUtil.sortMapByValueDescending(websiteCounts);
		Iterator<Entry<String, Integer>> it = sortedWebsiteCounts.entrySet().iterator();
		int count = 1;
		while(it.hasNext() && count <= 20) {
			String term = it.next().getKey();
			System.out.println("\t" + count + ". " + term + " (" + sortedWebsiteCounts.get(term) + " times)");
			count++;
		}
		System.out.println("\n");
	}


	public void frequencyOfURLs() {
		int queriesWithURLsCount = 0;

		for (List<String> row : data) {
			String query = row.get(3).trim();

			if (IRUtil.queryContainsURL(query)) {
				queriesWithURLsCount++;
			}
		}

		double percentageQueriesWithURLs = ((double) queriesWithURLsCount / numQueries) * 100;
		percentageQueriesWithURLs = Math.round(percentageQueriesWithURLs * 100.0) / 100.0;

		System.out.println("How often do URLs appear in queries?\n\t" + percentageQueriesWithURLs + "% of queries contain URLs\n\n");
	}


	public void meanAndMedian() {
		List<Integer> characterCounts = new ArrayList<>();
		List<Integer> wordCounts = new ArrayList<>();

		for (List<String> row : data) {
			String query = row.get(3).trim();
			List<String> tokens = IRUtil.tokenize(query);
			int characters = 0;

			for (String token : tokens) {
				characters += token.length();
			}

			characterCounts.add(characters);
			wordCounts.add(tokens.size());
		}

		Collections.sort(characterCounts);
		Collections.sort(wordCounts);

		double characterMedian;
		if (characterCounts.size() % 2 == 0)
			characterMedian = ((double)characterCounts.get(characterCounts.size() / 2) + (double)characterCounts.get(characterCounts.size() / 2 - 1))/2;
		else
			characterMedian = characterCounts.get(characterCounts.size() / 2);

		double wordMedian;
		if (wordCounts.size() % 2 == 0)
			wordMedian = ((double)wordCounts.get(wordCounts.size() / 2) + (double)wordCounts.get(wordCounts.size() / 2 - 1))/2;
		else
			wordMedian = wordCounts.get(wordCounts.size() / 2);


		double wordAverage = wordCounts
				.stream()
				.mapToDouble(a -> a)
				.average().getAsDouble();
		wordAverage = Math.round(wordAverage * 100.0) / 100.0;

		double characterAverage = characterCounts
				.stream()
				.mapToDouble(a -> a)
				.average().getAsDouble();
		characterAverage = Math.round(characterAverage * 100.0) / 100.0;


		System.out.println("Report the mean and median query length in both words and characters.");
		System.out.println("\twords:\n" + "\t\tmedian: " + wordMedian + "\n\t\tmean: " + wordAverage);
		System.out.println("\tcharacters:\n" + "\t\tmedian: " + characterMedian + "\n\t\tmean: " + characterAverage + "\n\n");
	}


	public void caseStatistics() {
		int mixedCaseCount = 0;
		int upperCaseCount = 0;
		int lowerCaseCount = 0;

		for (List<String> row : data) {
			String query = row.get(3).trim();

			if (query.toUpperCase().equals(query)) {
				upperCaseCount++;
			}
			else if (query.toLowerCase().equals(query)) {
				lowerCaseCount++;
			}
			else {
				mixedCaseCount++;
			}
		}

		double percentageUpperCase = ((double) upperCaseCount / numQueries) * 100;
		percentageUpperCase = Math.round(percentageUpperCase * 100.0) / 100.0;

		double percentageLowerCase = ((double) lowerCaseCount / numQueries) * 100;
		percentageLowerCase = Math.round(percentageLowerCase * 100.0) / 100.0;

		double percentageMixedCase = ((double) mixedCaseCount / numQueries) * 100;
		percentageMixedCase = Math.round(percentageMixedCase * 100.0) / 100.0;


		System.out.println("What percentage of queries are mixed case? All upper case? All lower case?");
		System.out.println("\tmixed case: " + percentageMixedCase + "%");
		System.out.println("\tupper case: " + percentageUpperCase + "%");
		System.out.println("\tlower case: " + percentageLowerCase + "%\n\n");
	}


	public static void main(String[] args) throws IOException {
		WebQueryLogAnalysis webQueryLogAnalysis = new WebQueryLogAnalysis();
		webQueryLogAnalysis.readTsvFile();

		webQueryLogAnalysis.averageQueriesPerUserId();
		webQueryLogAnalysis.percentageOfTopTenRequests();
		webQueryLogAnalysis.percentageAskedByOneUser();
		webQueryLogAnalysis.mostCommonQueries();
		webQueryLogAnalysis.mostCommonPhrasesInQueries();
		webQueryLogAnalysis.frequencyOfURLs();
		webQueryLogAnalysis.meanAndMedian();
		webQueryLogAnalysis.caseStatistics();
		webQueryLogAnalysis.mostCommonNonStopwords();
		webQueryLogAnalysis.queriesWithStopwords();
		webQueryLogAnalysis.mostCommonWebsites();
		webQueryLogAnalysis.frequencyOfQuerySyntax();
		webQueryLogAnalysis.queryVolume();
	}
}