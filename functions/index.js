const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const NEW_ORDER_TOPIC = "admin_orders";

// Fires whenever a new document is written to the "orders" collection (see
// db.collection("orders").document(orderId).set(newOrder) in MainApp.kt) and pushes a
// notification to every admin device subscribed to the "admin_orders" FCM topic.
exports.notifyAdminsOnNewOrder = onDocumentCreated("orders/{orderId}", async (event) => {
  const order = event.data?.data();
  if (!order) return;

  const client = order.client || "?";
  const totalQty = order.totalQty ?? "?";

  await getMessaging().send({
    topic: NEW_ORDER_TOPIC,
    notification: {
      title: "Nouvelle commande reçue",
      body: `Client: ${client} — Qté totale: ${totalQty}`,
    },
    data: {
      orderId: event.params.orderId,
    },
    android: {
      priority: "high",
    },
  });
});
