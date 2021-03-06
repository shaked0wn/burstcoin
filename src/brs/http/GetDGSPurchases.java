package brs.http;

import brs.DigitalGoodsStore;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.util.FilteringIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.*;

public final class GetDGSPurchases extends APIServlet.APIRequestHandler {

  static final GetDGSPurchases instance = new GetDGSPurchases();

  private GetDGSPurchases() {
    super(new APITag[] {APITag.DGS}, SELLER_PARAMETER, BUYER_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, COMPLETED_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long sellerId = ParameterParser.getSellerId(req);
    long buyerId = ParameterParser.getBuyerId(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);
    final boolean completed = "true".equalsIgnoreCase(req.getParameter(COMPLETED_PARAMETER));


    JSONObject response = new JSONObject();
    JSONArray purchasesJSON = new JSONArray();
    response.put("purchases", purchasesJSON);

    if (sellerId == 0 && buyerId == 0) {
      try (FilteringIterator<DigitalGoodsStore.Purchase> purchaseIterator
           = new FilteringIterator<>(DigitalGoodsStore.getAllPurchases(0, -1),
                                     new FilteringIterator.Filter<DigitalGoodsStore.Purchase>() {
                                       @Override
                                       public boolean ok(DigitalGoodsStore.Purchase purchase) {
                                         return ! (completed && purchase.isPending());
                                       }
                                     }, firstIndex, lastIndex)) {
        while (purchaseIterator.hasNext()) {
          purchasesJSON.add(JSONData.purchase(purchaseIterator.next()));
        }
      }
      return response;
    }

    BurstIterator<DigitalGoodsStore.Purchase> purchases;
    if (sellerId != 0 && buyerId == 0) {
      purchases = DigitalGoodsStore.getSellerPurchases(sellerId, 0, -1);
    } else if (sellerId == 0) {
      purchases = DigitalGoodsStore.getBuyerPurchases(buyerId, 0, -1);
    } else {
      purchases = DigitalGoodsStore.getSellerBuyerPurchases(sellerId, buyerId, 0, -1);
    }
    try (FilteringIterator<DigitalGoodsStore.Purchase> purchaseIterator
         = new FilteringIterator<>(purchases,
                                   new FilteringIterator.Filter<DigitalGoodsStore.Purchase>() {
                                     @Override
                                     public boolean ok(DigitalGoodsStore.Purchase purchase) {
                                       return ! (completed && purchase.isPending());
                                     }
                                   }, firstIndex, lastIndex)) {
      while (purchaseIterator.hasNext()) {
        purchasesJSON.add(JSONData.purchase(purchaseIterator.next()));
      }
    }
    return response;
  }
}
