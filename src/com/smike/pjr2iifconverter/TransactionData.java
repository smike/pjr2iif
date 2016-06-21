package com.smike.pjr2iifconverter;

import java.util.Date;

public class TransactionData {
  private Date receiptDate;
  private String transactionId;
  private float amount;
  private String account;

  public TransactionData(Date receiptDate, String transactionId, float amount, String account) {
    this.receiptDate = receiptDate;
    this.transactionId = transactionId;
    this.amount = amount;
    this.account = account;
  }

  public Date getReceiptDate() {
    return receiptDate;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public float getAmount() {
    return amount;
  }

  public String getAccount() {
    return account;
  }
  
  public String toString() {
	return "{receiptDate: " + this.getReceiptDate() +
			", transactionId: " + this.getTransactionId() +
			", amount: " + this.getAmount() +
			", account: " + this.getAccount() +
			"}";
  }
}
