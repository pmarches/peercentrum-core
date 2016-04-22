//package org.peercentrum.settlement;
//
//import static org.junit.Assert.assertTrue;
//
//import org.junit.Test;
//
//import com.google.bitcoin.core.Address;
//import com.google.bitcoin.core.NetworkParameters;
//import com.google.bitcoin.params.MainNetParams;
//
//public class SybillScoreTest {
//  static final NetworkParameters params=MainNetParams.get();
//
//  @Test
//  public void test() throws Exception {
//    SybillScore scorer=new SybillScore();
//    assertTrue(scorer.getScore(new Address(params, "1A8JiWcwvpY7tAopUkSnGuEYHmzGYfZPiq"))>=18485509572844l);
//    assertTrue(scorer.isAddressGenuine(new Address(params, "1A8JiWcwvpY7tAopUkSnGuEYHmzGYfZPiq"), true));
//  }
//
//}
