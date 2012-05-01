/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;

public class TestCollectionKeys {

  @Test
  public void testDefaultKeys() throws CryptoException, NoCollectionKeysSetException {
    CollectionKeys ck = new CollectionKeys();
    try {
      ck.defaultKeyBundle();
      fail("defaultKeys should throw.");
    } catch (NoCollectionKeysSetException ex) {
      // Good.
    }
    KeyBundle testKeys = KeyBundle.withRandomKeys();
    ck.setDefaultKeyBundle(testKeys);
    assertEquals(testKeys, ck.defaultKeyBundle());
  }

  @Test
  public void testKeyForCollection() throws CryptoException, NoCollectionKeysSetException {
    CollectionKeys ck = new CollectionKeys();
    try {
      ck.keyBundleForCollection("test");
      fail("keyForCollection should throw.");
    } catch (NoCollectionKeysSetException ex) {
      // Good.
    }
    KeyBundle testKeys  = KeyBundle.withRandomKeys();
    KeyBundle otherKeys = KeyBundle.withRandomKeys();

    ck.setDefaultKeyBundle(testKeys);
    assertEquals(testKeys, ck.defaultKeyBundle());
    assertEquals(testKeys, ck.keyBundleForCollection("test"));  // Returns default.

    ck.setKeyBundleForCollection("test", otherKeys);
    assertEquals(otherKeys, ck.keyBundleForCollection("test"));  // Returns default.

  }

  public static void assertSame(byte[] arrayOne, byte[] arrayTwo) {
    assertTrue(Arrays.equals(arrayOne, arrayTwo));
  }


  @Test
  public void testSetKeysFromWBO() throws IOException, ParseException, NonObjectJSONException, CryptoException, NoCollectionKeysSetException {
    String json = "{\"default\":[\"3fI6k1exImMgAKjilmMaAWxGqEIzFX/9K5EjEgH99vc=\",\"/AMaoCX4hzic28WY94XtokNi7N4T0nv+moS1y5wlbug=\"],\"collections\":{},\"collection\":\"crypto\",\"id\":\"keys\"}";
    CryptoRecord rec = new CryptoRecord(json);

    KeyBundle syncKeyBundle = new KeyBundle("slyjcrjednxd6rf4cr63vqilmkus6zbe", "6m8mv8ex2brqnrmsb9fjuvfg7y");
    rec.keyBundle = syncKeyBundle;

    rec.encrypt();
    CollectionKeys ck = new CollectionKeys();
    ck.setKeyPairsFromWBO(rec, syncKeyBundle);
    byte[] input = "3fI6k1exImMgAKjilmMaAWxGqEIzFX/9K5EjEgH99vc=".getBytes("UTF-8");
    byte[] expected = Base64.decodeBase64(input);
    assertSame(expected, ck.defaultKeyBundle().getEncryptionKey());
  }

  @Test
  public void testCryptoRecordFromCollectionKeys() throws CryptoException, NoCollectionKeysSetException, IOException, ParseException, NonObjectJSONException {
    CollectionKeys ck1 = CollectionKeys.generateCollectionKeys();
    assertNotNull(ck1.defaultKeyBundle());
    assertEquals(ck1.keyBundleForCollection("foobar"), ck1.defaultKeyBundle());
    CryptoRecord rec = ck1.asCryptoRecord();
    assertEquals(rec.collection, "crypto");
    assertEquals(rec.guid, "keys");
    JSONArray defaultKey = (JSONArray) rec.payload.get("default");

    assertSame(Base64.decodeBase64((String) (defaultKey.get(0))), ck1.defaultKeyBundle().getEncryptionKey());
    CollectionKeys ck2 = new CollectionKeys();
    ck2.setKeyPairsFromWBO(rec, null);
    assertSame(ck1.defaultKeyBundle().getEncryptionKey(), ck2.defaultKeyBundle().getEncryptionKey());
  }

  @Test
  public void testCreateKeysBundle() throws CryptoException, NonObjectJSONException, IOException, ParseException, NoCollectionKeysSetException {
    String username =                       "b6evr62dptbxz7fvebek7btljyu322wp";
    String friendlyBase32SyncKey =          "basuxv2426eqj7frhvpcwkavdi";

    KeyBundle syncKeyBundle = new KeyBundle(username, friendlyBase32SyncKey);

    CollectionKeys ck = CollectionKeys.generateCollectionKeys();
    CryptoRecord unencrypted = ck.asCryptoRecord();
    unencrypted.keyBundle = syncKeyBundle;
    CryptoRecord encrypted = unencrypted.encrypt();

    CollectionKeys ckDecrypted = new CollectionKeys();
    ckDecrypted.setKeyPairsFromWBO(encrypted, syncKeyBundle);

    // Compare decrypted keys to the keys that were set upon creation
    assertArrayEquals(ck.defaultKeyBundle().getEncryptionKey(), ckDecrypted.defaultKeyBundle().getEncryptionKey());
    assertArrayEquals(ck.defaultKeyBundle().getHMACKey(), ckDecrypted.defaultKeyBundle().getHMACKey());
  }

  @Test
  public void testDifferences() throws CryptoException {
    KeyBundle kb1 = KeyBundle.withRandomKeys();
    KeyBundle kb2 = KeyBundle.withRandomKeys();
    KeyBundle kb3 = KeyBundle.withRandomKeys();
    CollectionKeys a = CollectionKeys.generateCollectionKeys();
    CollectionKeys b = CollectionKeys.generateCollectionKeys();
    Set<String> diffs;

    a.setKeyBundleForCollection("1", kb1);
    b.setKeyBundleForCollection("1", kb1);
    diffs = CollectionKeys.differences(a, b);
    assertTrue(diffs.isEmpty());

    a.setKeyBundleForCollection("2", kb2);
    diffs = CollectionKeys.differences(a, b);
    assertArrayEquals(new String[] { "2" }, diffs.toArray(new String[diffs.size()]));

    b.setKeyBundleForCollection("3", kb3);
    diffs = CollectionKeys.differences(a, b);
    assertEquals(2, diffs.size());
    assertTrue(diffs.contains("2"));
    assertTrue(diffs.contains("3"));

    b.setKeyBundleForCollection("1", KeyBundle.withRandomKeys());
    diffs = CollectionKeys.differences(a, b);
    assertEquals(3, diffs.size());
  }
}
