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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVReader;

public class Pjr2IifConverter {
  private static final String TENDER_CODE_TAG = "TenderCode";
  private static final String RECEIPT_DATE_TAG = "ReceiptDate";
  private static final String TRANSACTION_ID_TAG = "TransactionID";
  private static final String TRANSACTION_TOTAL_NET_AMOUNT_TAG = "TransactionTotalNetAmount";
  private static final String ACCOUNT_ID_TAG = "AccountID";

  private static final String HOUSE_CHARGES = "houseCharges";

  private static final String IIF_HEADER =
      "!TRNS\tTRNSID\tTRNSTYPE\tDATE\tDOCNUM\tACCNT\tNAME\tAMOUNT\tPAID\n" +
      "!SPL\tTRNSID\tTRNSTYPE\tDATE\tACCNT\tAMOUNT\tCLEAR\n" +
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

  public String convert() throws ParserConfigurationException, IOException, SAXException, ParseException {
    documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    parseAccountIdMap(accountIdMapFile);

    Formatter formatter = new Formatter();
    formatter.format(IIF_HEADER);
    for (File xmlFile : xmlFiles) {
      TransactionData transactionData = parsePjrFile(xmlFile);

      // If the transaction is invalid, skip it over.
      if (transactionData == null) {
        continue;
      }

      formatter.format(IIF_TRANSACTION,
                       transactionData.getReceiptDate(),
                       transactionData.getTransactionId(),
                       transactionData.getAccount(),
                       transactionData.getAmount());
      transactions.add(transactionData);
    }

    return formatter.toString();
  }

  private TransactionData parsePjrFile(File file) throws SAXException, IOException, ParseException {
    Document document = documentBuilder.parse(file);

    String tenderCode = getFirstValueByTagName(TENDER_CODE_TAG, document);
    if (!HOUSE_CHARGES.equals(tenderCode)) {
      return null;
    }

    String receiptDateString = getFirstValueByTagName(RECEIPT_DATE_TAG, document);
    String transactionId = getFirstValueByTagName(TRANSACTION_ID_TAG, document);
    String transactionTotalNetAmount =
        getFirstValueByTagName(TRANSACTION_TOTAL_NET_AMOUNT_TAG, document);
    String accountId = getFirstValueByTagName(ACCOUNT_ID_TAG, document);

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

    // We only care about the part of the id before the first "-".
    accountId = accountId.substring(0, accountId.indexOf("-")).trim();
    String accountName = accountIdMap.get(accountId);

    return new TransactionData(receiptDate, transactionId, amount, accountName);
  }

  private void parseAccountIdMap(File file) throws IOException {
    FileReader fileReader = new FileReader(file);
    CSVReader csvReader = new CSVReader(fileReader);
    List<String[]> rows = csvReader.readAll();
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
      String xmlFile = "/Users/smike/Downloads/Passport"; //PJR3401106140611321090.xml";
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
      System.out.println(xmlConverter.convert());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
