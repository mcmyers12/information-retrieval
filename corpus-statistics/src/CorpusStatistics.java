import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorpusStatistics {

	private static final String BIBLE_FILE = "bible-asv.txt";
	private static final String LES_MIS_FILE = "lesmis.txt";

	private int numDocuments = 0; // Number of paragraphs processed
	private int vocabularySize = 0; // Number of unique words observed
	private int collectionSize = 0; // Total number of words encountered
	private int numWordsInOneDocument = 0; // Number of words that occur in exactly one document
	
	private Map<String, Term> lexicon = new HashMap<>();
	private List<Term> terms;

	private class Term {
		private String text;
		private int collectionFrequency = 0; // Number of times term is seen
		private int documentFrequency = 0; // Number of documents which the word occurs in
		
		public String toString() {
			return text + "\t\t - collection frequency: " + collectionFrequency + ", document frequency: " + documentFrequency;
		}
	}

	private void readFile(String fileName) {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(fileName);
			bufferedReader = new BufferedReader(fileReader);

			String currentLine;

			int count = 0;
			while ((currentLine = bufferedReader.readLine()) != null) { //) && count < 3) {
				if (currentLine.startsWith("<P ID=")) { // The start of a new document (paragraph)
					count ++;
					numDocuments++;
					currentLine = bufferedReader.readLine();
					Set<String> tokensInDocument = new HashSet<>();
					
					while (currentLine != null && !currentLine.startsWith("</P>")) {
						String[] tokens = tokenize(currentLine);
						
						for (String token : tokens) {
							collectionSize++;
							
							tokensInDocument.add(token);
							if (lexicon.containsKey(token)) {
								lexicon.get(token).collectionFrequency++; // Increment number of times term is seen
							}
							else {
								Term term = new Term();
								term.text = token;
								term.collectionFrequency++;
								lexicon.put(token, term);
							}
						}
						
						currentLine = bufferedReader.readLine();
					}
					
					for (String token : tokensInDocument) {
						lexicon.get(token).documentFrequency++; // Increment number of documents each token occurs in
					}
					
				}
			}
			
			vocabularySize = lexicon.keySet().size();
			
			getSortedTermList();
			calculateNumWordsInOneDocument();
			
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
	
	//Assumes sorted lexicon
	private void calculateNumWordsInOneDocument() {
		for (Term term : terms) {
			if (term.documentFrequency == 1) {
				numWordsInOneDocument++;
			}
		}
	}
	
	private void getSortedTermList() { 
	   /*List<Entry<String, Term>> list = new LinkedList<Entry<String, Term>>(lexicon.entrySet());
	   // Defined Custom Comparator here
	   Collections.sort(list, new Comparator<Entry<String, Term>>() {
	        public int compare(Entry<String, Term> o1, Entry<String, Term> o2) {
	           return ((Comparable<Integer>) ((Map.Entry<String, Term>) (o1)).getValue().collectionFrequency)
	              .compareTo(((Map.Entry<String, Term>) (o2)).getValue().collectionFrequency);
	        }
	   });
	   
	   // Here I am copying the sorted list in HashMap
	   // using LinkedHashMap to preserve the insertion order
	   HashMap<String, Term> sortedHashMap = new LinkedHashMap<>();
	   for (Iterator<Entry<String, Term>> it = list.iterator(); it.hasNext();) {
	          Map.Entry<String, Term> entry = (Map.Entry<String, Term>) it.next();
	          sortedHashMap.put(entry.getKey(), entry.getValue());
	   } 
	   lexicon = sortedHashMap;
	   terms = new ArrayList<Term>(sortedHashMap.values());*/
	   
	   
	   terms = new ArrayList<Term>(lexicon.values());
	   
	   Collections.sort(terms, new Comparator<Term>() {
		   public int compare(Term term1, Term term2) {
				return term1.collectionFrequency > term2.collectionFrequency ? -1: (term1.collectionFrequency < term2.collectionFrequency ? 1 : 0);
			}
	   });
	
	  }
	
	private void printResults() {
		/*for (Entry<String, Term> entry : lexicon.entrySet()) {
			System.out.println(entry.getKey());
			System.out.println("\tcollectionFrequency: " + entry.getValue().collectionFrequency);
			System.out.println("\tdocumentFrequency: " + entry.getValue().documentFrequency);
		}*/
		
		/*for (Term term: terms) {
			System.out.println(term);
		}*/
		
		System.out.println("\n\n\nNumber of documents (paragraphs) processed");
		System.out.println("\t" + numDocuments);
		System.out.println("\nVocabulary size (number of unique words observed)");
		System.out.println("\t" + vocabularySize);
		System.out.println("\nCollection size (total number of words encountered)");
		System.out.println("\t" + collectionSize);
		System.out.println("\nNumber of words uccuring in exactly one document");
		System.out.println("\t" + numWordsInOneDocument);
		
		System.out.println("\nMost frequent words");
		int count = 0;
		for (Term term : terms) {
			if (count == 30) {
				break;
			}
			System.out.println("\t" + count++ + ".\t" + term);
		}
		System.out.println("\t100.\t" + terms.get(99));
		System.out.println("\t500.\t" + terms.get(499));
		System.out.println("\t1000.\t" + terms.get(999));
		
		
		System.out.println();
		System.out.println();
		
	}

	/**
	 * Split on spaces, Lower case only if not all upper case, Remove leading
	 * and trailing punctuation from each token
	 * 
	 * @param line
	 * @return
	 */
	private String[] tokenize(String line) {
		String[] tokens = line.split("\\s+"); // Split string on whitespace
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = tokens[i].toLowerCase();
			tokens[i] = tokens[i].replaceFirst("^[^a-zA-Z]+", "");
			tokens[i] = tokens[i].replaceAll("[^a-zA-Z]+$", "");
		}

		return tokens;
	}

	// Calculate document frequency: number of documents a term appears in
	// Calculate collection frequency: number of times the word appears
	// Test program on two text files
	public static void main(String[] args) {
		CorpusStatistics corpusStatistics = new CorpusStatistics();
		corpusStatistics.readFile(CorpusStatistics.LES_MIS_FILE);
		corpusStatistics.printResults();
	}

}
