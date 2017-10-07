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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import edu.jhu.ir.documentsimilarity.IRUtil.InvertedFileRecord;
import edu.jhu.ir.documentsimilarity.IRUtil.Term;

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
	private boolean useStemming;
	private final int INT_SIZE = 4;  // Number of bytes per int written to the inverted file
	private final String INVERTED_FILENAME = "inverted-file.bin";
	private final String DICTIONARY_FILENAME = "dictionary.ser";
	private String inputFileName; // Name of input file for which to create inverted file
	private int numDocuments = 0; // Number of paragraphs processed
	private int vocabularySize = 0; // Number of unique words observed
	private Map<String, Term> lexicon = new HashMap<>();  // Lexicon that will hold all terms
	private Map<String, List<InvertedFileRecord>> invertedFileRecords = new TreeMap<>();	// Map containing each inverted file record sorted by term, then docId


	/**
	 * Given an input file name, builds an inverted index and lexicon on disk
	 * @param inputFileName
	 */
	public InvertedFileAccessor(String inputFileName, boolean useStemming) {
		this.inputFileName = inputFileName;
		this.useStemming = useStemming;

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


	/**
	 * Get the number of documents processed
	 * @return
	 */
	public int getNumDocuments() {
		return numDocuments;
	}


	/**
	 * Get the vocabulary size - number of unique words observed
	 * @return
	 */
	public int getVocabularySize() {
		return vocabularySize;
	}


	/**
	 * Retrieve the dictionary from disk
	 * @return
	 */
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
			int documentFrequency = term.getDocumentFrequency() * 2;  // The number of integers stored for the token

			int filePointer = term.getInvertedFileLocation();

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
	 * Read the input file and build the corresponding lexicon
	 */
	private void buildLexicon() {
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
					List<String> tokens = IRUtil.tokenize(currentLine);

					for (String token : tokens) {
						if (useStemming) {
							if (token.length() > 5) {
								token = token.substring(0, 5);
							}
						}
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
						Term term = lexicon.get(token);
						int documentFrequency = term.getDocumentFrequency();
						term.setDocumentFrequency(++documentFrequency); // Increment number of documents each token occurs in
					}
					else {
						Term term = new Term();
						term.setText(token);
						int documentFrequency = term.getDocumentFrequency();
						term.setDocumentFrequency(++documentFrequency);
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
	private void createInvertedIndex() {
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(INVERTED_FILENAME, "rw"); // Open inverted binary file for writing
			int filePointer = 0;
			for (Entry<String, List<InvertedFileRecord>> invertedFileRecordsEntry: invertedFileRecords.entrySet()) {
				String token = invertedFileRecordsEntry.getKey();
				List<InvertedFileRecord> invertedFileRecordList = invertedFileRecordsEntry.getValue();

				lexicon.get(token).setInvertedFileLocation(filePointer);

				for (InvertedFileRecord invertedFileRecord : invertedFileRecordList) {
					randomAccessFile.writeInt(invertedFileRecord.getDocumentId());
					filePointer += INT_SIZE;

					randomAccessFile.writeInt(invertedFileRecord.getTermFrequency());
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
	private void writeDictionaryToFile() throws IOException {
		FileOutputStream fos = new FileOutputStream(DICTIONARY_FILENAME);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(lexicon);
		oos.close();
	}


	/**
	 * Get and display the file size information for the dictionary file and the inverted file
	 * Determine whether the original text or the index takes more space and print this information
	 * Determine whether the dictionary or inverted file takes more space and print this information
	 */
	public void printFileSizeInformation() {
		File dictionaryFile = new File(DICTIONARY_FILENAME);
		long dictionaryFileSize = dictionaryFile.length();

		System.out.print("Dictionary file size in GB: ");
		System.out.printf("%.9f", (double) dictionaryFileSize / 1000000000);

		File invertedFile = new File(INVERTED_FILENAME);
		long invertedFileSize = invertedFile.length();
		System.out.println("\n\nInverted file size in GB: " + (double) invertedFileSize  / 1000000000);
	}
}



