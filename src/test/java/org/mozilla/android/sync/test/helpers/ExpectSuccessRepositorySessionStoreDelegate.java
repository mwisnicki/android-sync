package org.mozilla.android.sync.test.helpers;

import java.util.concurrent.ExecutorService;

import junit.framework.AssertionFailedError;

import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class ExpectSuccessRepositorySessionStoreDelegate extends
    ExpectSuccessDelegate implements RepositorySessionStoreDelegate {

  public ExpectSuccessRepositorySessionStoreDelegate(WaitHelper waitHelper) {
    super(waitHelper);
  }

  @Override
  public void onRecordStoreFailed(Exception ex) {
    log("Record store failed.", ex);
    performNotify(new AssertionFailedError("onRecordStoreFailed: record store should not have failed."));
  }

  @Override
  public void onRecordStoreSucceeded(Record record) {
    log("Record store succeeded.");
  }

  @Override
  public void onStoreCompleted(long storeEnd) {
    log("Record store completed at " + storeEnd);
  }

  @Override
  public RepositorySessionStoreDelegate deferredStoreDelegate(ExecutorService executor) {
    return this;
  }
}