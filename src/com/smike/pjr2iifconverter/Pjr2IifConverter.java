package com.smike.pjr2iifconverter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.opencsv.exceptions.CsvException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.opencsv.CSVReader;

public class Pjr2IifConverter {
  private static Logger logger = Logger.getLogger(Pjr2IifConverter.class.getName());

  private static final String RECEIPT_DATE_TAG = "ReceiptDate";
  private static final String TRANSACTION_ID_TAG = "TransactionID";
  private static final String TRANSACTION_TOTAL_NET_AMOUNT_TAG = "TransactionTotalNetAmount";
  private static final String ACCOUNT_ID_TAG = "AccountID";

  private static final String IIF_HEADER =
      "!TRNS\tTRNSID\tTRNSTYPE\tDATE\tDOCNUM\tACCNT\tNAME\tAMOUNT\tPAID\n" +
      "!SPL\tSPLID\tTRNSTYPE\tDATE\tACCNT\tAMOUNT\tCLEAR\n" +
      "!ENDTRNS\n";
  private static final String IIF_TRANSACTION =
      "\nTRNS\t\tINVOICE\t%tD\t%s\tAccounts Receivable\t%s\t%.2f\tN\n" +
      "SPL\t\tINVOICE\t%1$tD\tSales:Local Account Sales\t-%4$.2f\tN\n" +
      "ENDTRNS\n";

  private DocumentBuilder documentBuilder;
  private Map<String, String> accountIdMap = new HashMap<String, String>();
  private List<TransactionData> transactions = new LinkedList<TransactionData>();

  private List<File> xmlFiles = new LinkedList<File>();
  private File accountIdMapFile;

  public Pjr2IifConverter(List<File> xmlFiles, File accountIdMapFile){
    this.xmlFiles.addAll(xmlFiles);
    this.accountIdMapFile = accountIdMapFile;
  }

  public String convert(boolean ignoreNegativeTransactions) throws Exception {
    documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    parseAccountIdMap(accountIdMapFile);

    Formatter formatter = new Formatter();
    formatter.format(IIF_HEADER);
    for (File xmlFile : xmlFiles) {
      TransactionData transactionData = parsePjrFile(xmlFile);
      System.out.println(xmlFile + ": " + transactionData);

      // If the transaction is invalid, skip it over.
      if (transactionData == null ||
          (ignoreNegativeTransactions && transactionData.getAmount() < 0)) {
        continue;
      }

      formatter.format(IIF_TRANSACTION,
                       transactionData.getReceiptDate(),
                       transactionData.getTransactionId(),
                       transactionData.getAccount(),
                       transactionData.getAmount());
      transactions.add(transactionData);
    }

    String output = formatter.toString();
    formatter.close();
    return output;
  }

  private String getAccountName(String accountId) {
    if (accountId == null || accountId.isEmpty()) {
      return null;
    }

    // We only care about the part of the id before the first "-".
    int indexOfDash = accountId.indexOf("-");
    if (indexOfDash == -1) {
      return null;
    }
    accountId = accountId.substring(0, indexOfDash).trim();

    // If we don't know about this accountId it won't be in the map and we can ignore it.
    return accountIdMap.get(accountId);
  }

  private TransactionData parsePjrFile(File file) throws SAXException, IOException, ParseException {
    Document document = documentBuilder.parse(file);
    System.out.println("Parsing " + file);

    String receiptDateString = getFirstValueByTagName(RECEIPT_DATE_TAG, document);
    String transactionId = getFirstValueByTagName(TRANSACTION_ID_TAG, document);
    String transactionTotalNetAmount =
        getFirstValueByTagName(TRANSACTION_TOTAL_NET_AMOUNT_TAG, document);
    String accountId = getFirstValueByTagName(ACCOUNT_ID_TAG, document);

    String accountName = this.getAccountName(accountId);
    if (accountName == null || transactionTotalNetAmount == null) {
      // We only care about transactions with known accounts and transaction amounts.
      logger.info("Skipping " + file + " because " +
        (accountName == null ? "account name" : "net amount") + " not found.");
      return null;
    }

    // Parse the date. It should be in YYYY-MM-DD format.
    String[] dateParts = receiptDateString.split("-");
    if (dateParts.length != 3) {
      throw new RuntimeException("Unable to parse date from " + file + ": " + receiptDateString);
    }
    int year = Integer.parseInt(dateParts[0]);
    int month = Integer.parseInt(dateParts[1]);
    int date = Integer.parseInt(dateParts[2]);
    Calendar calendar = Calendar.getInstance();
    // Date and month are 0-based
    calendar.set(year, month-1, date);
    Date receiptDate = calendar.getTime();

    float amount = Float.parseFloat(transactionTotalNetAmount);

    return new TransactionData(receiptDate, transactionId, amount, accountName);
  }

  private void parseAccountIdMap(File file) throws IOException, CsvException {
    FileReader fileReader = new FileReader(file);
    CSVReader csvReader = new CSVReader(fileReader);
    List<String[]> rows = csvReader.readAll();
    csvReader.close();
    for (String[] columns : rows) {
      if (columns.length != 2) {
        throw new RuntimeException("Unable to parse account ID map file because a line was in an unexpected format: " + columns);
      }
      String customerIdString = columns[0];
      String customerName = columns[1].trim();

      // Sometimes, multiple ids can be mapped to one name
      for (String customerId : customerIdString.split(",")) {
        accountIdMap.put(customerId.trim(), customerName);
      }
    }

    System.out.println(accountIdMap);
  }

  private String getFirstValueByTagName(String tagName, Document document) {
    NodeList nodeList = document.getElementsByTagName(tagName);

    if (nodeList.getLength() == 0) {
      return null;
    }

    Node node = nodeList.item(0);
    return node.getTextContent();
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      String xmlFile = "/Users/smike/Downloads/PJR"; //PJR3401106140611321090.xml";
      String accountIdMapFile = "/Users/smike/Downloads/accid.csv";

      File pjrFile = new File(xmlFile);
      List<File> xmlFiles = new LinkedList<File>();
      xmlFiles.add(pjrFile);
      if (pjrFile.isDirectory()) {
        xmlFiles.clear();
        File[] files = pjrFile.listFiles();
        for (File file : files) {
          xmlFiles.add(file);
        }
      }

      Pjr2IifConverter xmlConverter = new Pjr2IifConverter(xmlFiles, new File(accountIdMapFile));
      System.out.println(xmlConverter.convert(false));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
