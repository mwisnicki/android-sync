/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.jpake.stage;

import java.io.UnsupportedEncodingException;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.jpake.JPakeClient;
import org.mozilla.gecko.sync.setup.Constants;

import android.util.Log;

public class ComputeKeyVerificationStage implements JPakeStage {
  private final String LOG_TAG = "ComputeKeyVerificationStage";

  @Override
  public void execute(JPakeClient jClient) {
    Log.d(LOG_TAG, "Computing verification to send.");
    if (jClient.myKeyBundle == null) {
      Log.e(LOG_TAG, "KeyBundle has not been set; aborting.");
      jClient.abort(Constants.JPAKE_ERROR_INTERNAL);
    }
    try {
      jClient.jOutgoing = computeKeyVerification(jClient.myKeyBundle, jClient.mySignerId);
    } catch (UnsupportedEncodingException e) {
      Log.e(LOG_TAG, "Failure in key verification.", e);
      jClient.abort(Constants.JPAKE_ERROR_INVALID);
      return;
    } catch (CryptoException e) {
      Log.e(LOG_TAG, "Encryption failure in key verification.", e);
      jClient.abort(Constants.JPAKE_ERROR_INTERNAL);
      return;
    }

    jClient.runNextStage();
  }

  /*
   * Helper function to compute a ciphertext, IV, and HMAC from derived
   * keyBundle for verifying other party.
   *
   * (Made 'public' for testing and is a stateless function.)
   */
  public ExtendedJSONObject computeKeyVerification(KeyBundle keyBundle, String signerId)
      throws UnsupportedEncodingException, CryptoException
  {
    Log.d(LOG_TAG, "Encrypting key verification value.");
    // Encrypt payload with keys, and include HMAC because we're acting as the "sender."
    ExtendedJSONObject jPayload = JPakeClient.encryptPayload(JPakeClient.JPAKE_VERIFY_VALUE, keyBundle, true);
    ExtendedJSONObject result = new ExtendedJSONObject();
    result.put(Constants.JSON_KEY_TYPE, signerId + "3");
    result.put(Constants.JSON_KEY_VERSION, JPakeClient.KEYEXCHANGE_VERSION);
    result.put(Constants.JSON_KEY_PAYLOAD, jPayload.object);
    return result;
  }

}