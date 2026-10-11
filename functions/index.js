const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const NEW_ORDER_TOPIC = "admin_orders";

// Fires whenever a new document is created in "orders" (from the app or the website) and
// pushes a "new order" notification to every phone subscribed to the "admin_orders" topic
// (profiles that prepare orders, see MainApp.kt). Google delivers it even when the app is
// closed. Deliberately no client, quantity or price in it: it only says an order arrived.
// Region: the Firestore database is in eur3, and 2nd-gen Firestore triggers must run in a
// region of the database's location.
exports.notifyAdminsOnNewOrder = onDocumentCreated(
  { document: "orders/{orderId}", region: "europe-west1" },
  async (event) => {
    if (!event.data) return;

    await getMessaging().send({
      topic: NEW_ORDER_TOPIC,
      data: { orderId: event.params.orderId },
      android: {
        priority: "high",
        notification: {
          channelId: "new_orders",
          icon: "ic_stat_order",
          // Same tag as the app's own notification (NewOrderNotifier.kt), so a new one
          // replaces the previous instead of stacking up.
          tag: "new_order",
          // Texts come from the app's own translated strings; title/body are the fallback.
          titleLocKey: "notif_new_order_title",
          bodyLocKey: "notif_new_order_body",
          title: "Nouvelle commande reçue",
          body: "Une nouvelle commande vient d'être enregistrée.",
        },
      },
    });
  }
);
