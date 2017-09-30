package edu.jhu.ir.documentsimilarity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This class builds an inverted file that contains a postings list for each dictionary term
 * The inverted file is a binary file
 * It also writes the dictionary to disk, which is in the form of a serialized object
 * For each word in the dictionary, a file offset to the corresponding on-disk posting list is stored
 *
 * This program follows the memory-based inversion algorithm (Algorithm A)
 * 		It writes out the postings after all documents have been read
 * @author Miranda Myers
 *
 */
public class InvertedFileAccessor {
	private final int INT_SIZE = 4;  // Number of bytes per int written to the inverted file
	private final String INVERTED_FILENAME = "inverted-file.bin";
	private final String DICTIONARY_FILENAME = "dictionary.ser";
	private String inputFileName; // Name of input file for which to create inverted file
	private int numDocuments = 0; // Number of paragraphs processed
	private int vocabularySize = 0; // Number of unique words observed
	private int collectionSize = 0; // Total number of words encountered
	private Map<String, Term> lexicon = new HashMap<>();  // Lexicon that will hold all terms
	private Map<String, List<InvertedFileRecord>> invertedFileRecords = new TreeMap<>();	// Map containing each inverted file record sorted by term, then docId


	/**
	 * Given an input file name, builds an inverted index and lexicon on disk
	 * @param inputFileName
	 */
	public InvertedFileAccessor(String inputFileName) {
		this.inputFileName = inputFileName;

		//Build the dictionary
		buildLexicon();

		//Create the inverted file and write to binary file
		createInvertedIndex();

		//Write the lexicon to disk
		try {
			writeDictionaryToFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Term> getLexiconFromDisk() {
		ObjectInput input = null;
		try {
			InputStream file = new FileInputStream(DICTIONARY_FILENAME);
			InputStream buffer = new BufferedInputStream(file);
			input = new ObjectInputStream (buffer);
			return (Map<String, Term>) input.readObject();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return null;
	}

	/**
	 * Given a token, access the corresponding records for that token in the inverted file
	 * Return a list of InvertedFileRecord objects that contain the postings list and
	 * document counts for the token
	 * @throws IOException
	 */
	public List<InvertedFileRecord> readInvertedIndex(String token) throws IOException {
		List<InvertedFileRecord> invertedFileRecordList = new ArrayList<>();
		RandomAccessFile randomAccessFile = new RandomAccessFile(INVERTED_FILENAME, "r");

		Term term = lexicon.get(token);

		if (term != null) {
			int documentFrequency = term.documentFrequency * 2;  // The number of integers stored for the token

			int filePointer = term.invertedFileLocation;

			for (int i = 0; i < documentFrequency; i+=2) {
				randomAccessFile.seek(filePointer);
				int documentId = randomAccessFile.readInt();
				filePointer += INT_SIZE;

				randomAccessFile.seek(filePointer);
				int count = randomAccessFile.readInt();
				filePointer += INT_SIZE;

				invertedFileRecordList.add(new InvertedFileRecord(documentId, count));
			}
		}
		randomAccessFile.close();
		return invertedFileRecordList;
	}


	/**
	 * Class representing a term that is used to build the lexicon and list of terms
	 */
	public static class Term implements Serializable {
		private static final long serialVersionUID = -1947264671039701464L;
		private String text;	//Required in memory
		private int invertedFileLocation;	//Required in memory

		private int documentFrequency = 0; // Number of documents which the word occurs in	// Useful in memory

		@Override
		public String toString() {
			return text + "\t\t - document frequency: " + documentFrequency;
		}
	}


	/**
	 * Class representing an inverted file record that stores the document ID and
	 * count for the number of times a term appears in that document
	 * This class is used to hold the information that is stored to the inverted file
	 */
	private class InvertedFileRecord {
		private int documentId;
		private int count;

		public InvertedFileRecord(int documentId, int count) {
			this.documentId = documentId;
			this.count = count;
		}

		@Override
		public String toString() {
			return "(documentID: " + documentId
					+ ", count: " + count + ")";
		}
	}


	/**
	 * Read the input file and build the corresponding lexicon
	 */
	public void buildLexicon() {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(inputFileName);
			bufferedReader = new BufferedReader(fileReader);

			buildLexicon(bufferedReader);

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


	/**
	 * Build lexicon helper
	 * Build the lexicon
	 * Calculate collection size and total number of documents
	 * Calculate document frequency for each term
	 * Build a TreeMap containing inverted file records sorted by term then by docID
	 * @param bufferedReader
	 * @throws IOException
	 */
	private void buildLexicon(BufferedReader bufferedReader) throws IOException {
		String currentLine;

		while ((currentLine = bufferedReader.readLine()) != null) {
			if (currentLine.startsWith("<P ID=")) { // The start of a new document (paragraph)
				numDocuments++;
				int documentId = Integer.parseInt(currentLine.replace("<P ID=", "").replace(">", ""));

				currentLine = bufferedReader.readLine();
				Map<String, Integer> tokensInDocument = new HashMap<>();

				while (currentLine != null && !currentLine.startsWith("</P>")) {
					List<String> tokens = ParsingUtil.tokenize(currentLine);

					for (String token : tokens) {
						collectionSize++;

						if (tokensInDocument.containsKey(token)) {
							int count = tokensInDocument.get(token).intValue();
							tokensInDocument.put(token, ++count);
						}
						else {
							tokensInDocument.put(token, 1);
						}
					}

					currentLine = bufferedReader.readLine();
				}

				for (String token : tokensInDocument.keySet()) {
					if (lexicon.containsKey(token)) {
						lexicon.get(token).documentFrequency++; // Increment number of documents each token occurs in
					}
					else {
						Term term = new Term();
						term.text = token;
						term.documentFrequency++;
						lexicon.put(token, term);
					}

					int count = tokensInDocument.get(token);

					if (!invertedFileRecords.containsKey(token)) {
						invertedFileRecords.put(token, new ArrayList<InvertedFileRecord>());
					}
					invertedFileRecords.get(token).add(new InvertedFileRecord(documentId, count));
				}
			}
		}
		vocabularySize = lexicon.keySet().size();
	}


	/**
	 * Write to a binary file using 4-byte integers for document ids
	 * and 4-byte integers for document term frequency
	 */
	public void createInvertedIndex() {
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(INVERTED_FILENAME, "rw"); // Open inverted binary file for writing
			int filePointer = 0;
			for (Entry<String, List<InvertedFileRecord>> invertedFileRecordsEntry: invertedFileRecords.entrySet()) {
				String token = invertedFileRecordsEntry.getKey();
				List<InvertedFileRecord> invertedFileRecordList = invertedFileRecordsEntry.getValue();

				lexicon.get(token).invertedFileLocation = filePointer;

				for (InvertedFileRecord invertedFileRecord : invertedFileRecordList) {
					randomAccessFile.writeInt(invertedFileRecord.documentId);
					filePointer += INT_SIZE;

					randomAccessFile.writeInt(invertedFileRecord.count);
					filePointer += INT_SIZE;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (randomAccessFile != null) {
					randomAccessFile.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * Write the dictionary to a file as a serialized object
	 * @throws IOException
	 */
	public void writeDictionaryToFile() throws IOException {
		FileOutputStream fos = new FileOutputStream(DICTIONARY_FILENAME);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(lexicon);
		oos.close();
	}


	//TODO probably don't need
	/**
	 * Read in the serialized dictionary from disk
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public void readDictionaryFromFile() throws IOException, ClassNotFoundException {
		InputStream file = new FileInputStream(DICTIONARY_FILENAME);
		InputStream buffer = new BufferedInputStream(file);
		ObjectInput input = new ObjectInputStream (buffer);
		lexicon = (Map<String, Term>) input.readObject();
		input.close();
	}


	/**
	 * Print a list of InvertedFileRecord objects, on record per line
	 * If the list is empty, print none
	 * @param invertedFileRecordList
	 */
	public void prettyPrintList(List<InvertedFileRecord> invertedFileRecordList) {
		if (invertedFileRecordList.isEmpty()) {
			System.out.println("\tnone");
		}
		else {
			for (InvertedFileRecord invertedFileRecord : invertedFileRecordList) {
				System.out.println("\t" + invertedFileRecord);
			}
		}
	}


	/**
	 * Get and display the file size information for the dictionary file and the inverted file
	 * Determine whether the original text or the index takes more space and print this information
	 * Determine whether the dictionary or inverted file takes more space and print this information
	 */
	public void printFileSizeInformation() {
		File dictionaryFile = new File(DICTIONARY_FILENAME);
		long dictionaryFileSize = dictionaryFile.length();
		System.out.println("Dictionary file size in bytes: " + dictionaryFileSize);

		File invertedFile = new File(INVERTED_FILENAME);
		long invertedFileSize = invertedFile.length();
		System.out.println("\nInverted file size in bytes: " + invertedFileSize);

		File inputFile = new File(inputFileName);
		long inputFileSize = inputFile.length();
		long indexSize = dictionaryFileSize + invertedFileSize;

		if (inputFileSize > indexSize) {
			System.out.println("\nThe original text takes up more space than the index");
		}
		else {
			System.out.println("\nThe index takes up more space than the original text");
		}

		if (invertedFileSize > dictionaryFileSize) {
			System.out.println("\nThe inverted file takes up more space than the dictionary file");
		}
		else {
			System.out.println("\nThe dictionary file takes up more space than the inverted file");
		}
	}


	/**
	 * Execute test cases using the inverted file
	 * Print the number of documents, vocabulary size, and collection size
	 * Print out the document frequency and posting list for terms: study, plutonium, Rome, Athens, feasts
	 * Print out the document frequency for terms: horse, lovingkindness, Mary, dance
	 * @throws IOException
	 */
	public void testInvertedFile() throws IOException {
		System.out.println("---------------------------------" + "Statistics for file " + inputFileName + "---------------------------------");
		System.out.println("\nNumber of documents (paragraphs) processed");
		System.out.println("\t" + numDocuments);
		System.out.println("\nVocabulary size (number of unique words observed)");
		System.out.println("\t" + vocabularySize);
		System.out.println("\nCollection size (total number of words encountered)");
		System.out.println("\t" + collectionSize);

		System.out.println("\nExample document frequencies and postings lists:");
		System.out.println("  less");
		prettyPrintList(readInvertedIndex("less"));
		System.out.println("  sleep");
		prettyPrintList(readInvertedIndex("sleep"));
		System.out.println("  expand");
		prettyPrintList(readInvertedIndex("expand"));
		System.out.println("  a");
		prettyPrintList(readInvertedIndex("a"));

		System.out.println("\nExample document frequencies:");
		System.out.println("  a\n\t" + lexicon.get("a").documentFrequency);
		System.out.println("  the\n\t" + lexicon.get("the").documentFrequency);
	}


	/**
	 * Test InvertedFileBuilder
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void testInvertedFileBuilder() throws ClassNotFoundException, IOException {
		//Build the dictionary
		buildLexicon();

		//Create the inverted file and write to binary file
		createInvertedIndex();

		//Write the lexicon to disk
		writeDictionaryToFile();

		//Read the lexicon from disk
		readDictionaryFromFile();

		//Read test terms, postings lists, and document frequencies from the inverted file
		testInvertedFile();

		//Print the file sizes for the dictioanry and inverted file in bytes
		printFileSizeInformation();
	}


	/*public static void main(String[] args) throws IOException, ClassNotFoundException {
		InvertedFileAccessor invertedFileBuilder = new InvertedFileAccessor("fire10TEST.en.utf8");
		invertedFileBuilder.testInvertedFileBuilder();;
	}*/
}



