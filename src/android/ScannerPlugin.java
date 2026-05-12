package com.guilherme.scanner; // IMPORTANTE: Esse é o seu package

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import android.content.Intent;
import android.app.Activity;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScannerPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;
    private static final int SCAN_REQUEST_CODE = 101;

    // 1. Esse método é a "porta de entrada" que o JavaScript chama
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("startScan")) {
            this.callbackContext = callbackContext;
            startScan();
            return true;
        }
        return false;
    }

    private void startScan() {
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setGalleryImportAllowed(true)
            .build();

        GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);
        
        // 'cordova.getActivity()' é como acessamos a tela atual no OutSystems
        scanner.getStartScanIntent(cordova.getActivity())
            .addOnSuccessListener(intentSender -> {
                try {
                    cordova.setActivityResultCallback(this); // Prepara para receber o resultado
                    cordova.getActivity().startIntentSenderForResult(intentSender, SCAN_REQUEST_CODE, null, 0, 0, 0);
                } catch (Exception e) {
                    callbackContext.error("Erro ao abrir scanner: " + e.getMessage());
                }
            })
            .addOnFailureListener(e -> callbackContext.error("Falha no ML Kit: " + e.getMessage()));
    }

    // 2. Esse método captura o que o scanner retornou (PDF/Imagens)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult result = GmsDocumentScanningResult.fromActivityResultIntent(data);
            if (result != null) {
                try {
                    JSONObject response = new JSONObject();
                    // Se você pediu PDF, ele vem aqui:
                    if (result.getPdf() != null) {
                        response.put("pdfUri", result.getPdf().getUri().toString());
                    }
                    // Se quiser as imagens individuais:
                    if (result.getPages() != null && !result.getPages().isEmpty()) {
                        response.put("imageUri", result.getPages().get(0).getImageUri().toString());
                    }
                    
                    callbackContext.success(response); // Devolve para o OutSystems!
                } catch (JSONException e) {
                    callbackContext.error("Erro ao processar JSON");
                }
            }
        }
    }
}
