const functions = require("firebase-functions");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

exports.createUserWithRole = functions.https.onCall(async (data, context) => {
  try {
    console.log("INICIO DE FUNCIÓN");
    console.log("Timestamp:", new Date().toISOString());

    if (context.auth) {
      console.log("Usuario autenticado:", context.auth.uid);
    } else {
      console.log("Usuario NO autenticado");
    }

    const actualData = data.data || data;

    console.log("Datos recibidos:", {
      email: actualData.email,
      nombre: actualData.nombre,
      apellido: actualData.apellido,
      phone: actualData.phone,
      nombreEmpresa: actualData.nombreEmpresa,
      rol: actualData.rol,
      ruta: actualData.ruta,
      hasPassword: !!actualData.password,
    });

    if (!actualData) {
      throw new functions.https.HttpsError("invalid-argument", "No data received");
    }

    const email = actualData.email?.toString().trim() || "";
    const password = actualData.password?.toString().trim() || "";
    const nombre = actualData.nombre?.toString().trim() || "";
    const apellido = actualData.apellido?.toString().trim() || "";
    const phone = actualData.phone?.toString().trim() || "";
    const nombreEmpresa = actualData.nombreEmpresa?.toString().trim() || "";
    const rol = actualData.rol?.toString().trim() || "";
    const ruta = actualData.ruta?.toString().trim() || "";

    console.log("Datos procesados:");
    console.log("- email:", email);
    console.log("- password:", password ? "***PRESENTE***" : "AUSENTE");
    console.log("- nombre:", nombre);
    console.log("- apellido:", apellido);
    console.log("- phone:", phone);
    console.log("- nombreEmpresa:", nombreEmpresa);
    console.log("- rol:", rol);
    console.log("- ruta:", ruta);

    if (!email) throw new functions.https.HttpsError("invalid-argument", "Email requerido");
    if (!password) throw new functions.https.HttpsError("invalid-argument", "Password requerido");
    if (!nombre) throw new functions.https.HttpsError("invalid-argument", "Nombre requerido");
    if (!apellido) throw new functions.https.HttpsError("invalid-argument", "Apellido requerido");
    if (!phone) throw new functions.https.HttpsError("invalid-argument", "Phone requerido");
    if (!nombreEmpresa) throw new functions.https.HttpsError("invalid-argument", "NombreEmpresa requerido");
    if (!rol) throw new functions.https.HttpsError("invalid-argument", "Rol requerido");

    if (rol === "Motorizado" && !ruta) {
      throw new functions.https.HttpsError("invalid-argument", "Ruta requerida para Motorizado");
    }

    console.log("Validación completada");

    console.log("Creando usuario en Firebase Auth...");
    let userRecord;
    try {
      userRecord = await admin.auth().createUser({
        email: email,
        password: password,
      });
      console.log("Usuario creado en Auth:", userRecord.uid);
    } catch (authError) {
      console.error("Error en Firebase Auth:", authError);
      throw new functions.https.HttpsError("internal", `Error Auth: ${authError.message}`);
    }

    console.log("Preparando datos para Firestore...");
    const usuarioData = {
      nombre: nombre,
      apellido: apellido,
      email: email,
      nombreEmpresa: nombreEmpresa,
      phone: phone,
      rol: rol,
      ruta: rol === "Motorizado" ? ruta : "",
      fechaCreacion: admin.firestore.FieldValue.serverTimestamp(),
    };

    console.log("Datos para Firestore:", usuarioData);

    console.log("Guardando en Firestore...");
    try {
      await admin.firestore()
          .collection("usuarios")
          .doc(userRecord.uid)
          .set(usuarioData);
      console.log("Guardado en Firestore exitosamente");
    } catch (firestoreError) {
      console.error("Error en Firestore:", firestoreError);
      try {
        await admin.auth().deleteUser(userRecord.uid);
        console.log("Usuario eliminado de Auth debido a error en Firestore");
      } catch (deleteError) {
        console.error("Error eliminando usuario de Auth:", deleteError);
      }
      throw new functions.https.HttpsError("internal", `Error Firestore: ${firestoreError.message}`);
    }

    console.log("Función completada exitosamente");
    return {
      success: true,
      message: "Usuario creado con éxito",
      uid: userRecord.uid,
    };
  } catch (error) {
    console.error("ERROR GENERAL:", error.message);
    console.error("Error code:", error.code);

    if (error instanceof functions.https.HttpsError) {
      throw error;
    }

    throw new functions.https.HttpsError("internal", `Error inesperado: ${error.message}`);
  }
});
