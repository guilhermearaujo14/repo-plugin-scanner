package com.guilherme.scanner;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScannerPlugin extends CordovaPlugin {

    private static final String TAG = "MLKIT_PLUGIN";

    private static final int SCAN_REQUEST_CODE = 101;

    private CallbackContext callbackContext;

    @Override
    public boolean execute(
            String action,
            JSONArray args,
            CallbackContext callbackContext
    ) throws JSONException {

        if ("startScan".equals(action)) {

            this.callbackContext = callbackContext;

            // Mantém callback vivo no OutSystems
            PluginResult pluginResult =
                    new PluginResult(PluginResult.Status.NO_RESULT);

            pluginResult.setKeepCallback(true);

            callbackContext.sendPluginResult(pluginResult);

            startScan();

            return true;
        }

        return false;
    }

    private void startScan() {

        try {

            GmsDocumentScannerOptions options =
                    new GmsDocumentScannerOptions.Builder()
                            .setScannerMode(
                                    GmsDocumentScannerOptions.SCANNER_MODE_FULL
                            )
                            .setResultFormats(
                                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                            )
                            .setGalleryImportAllowed(true)
                            .setPageLimit(10)
                            .build();

            GmsDocumentScanner scanner =
                    GmsDocumentScanning.getClient(options);

            scanner.getStartScanIntent(cordova.getActivity())

                    .addOnSuccessListener(intentSender -> {

                        try {

                            Log.d(TAG, "Abrindo scanner");

                            // MUITO IMPORTANTE NO OUTSYSTEMS
                            cordova.setActivityResultCallback(
                                    ScannerPlugin.this
                            );

                            Intent fillInIntent = new Intent();

                            cordova.getActivity()
                                    .startIntentSenderForResult(
                                            intentSender,
                                            SCAN_REQUEST_CODE,
                                            fillInIntent,
                                            0,
                                            0,
                                            0
                                    );

                        } catch (Exception e) {

                            Log.e(
                                    TAG,
                                    "Erro ao abrir scanner",
                                    e
                            );

                            if (callbackContext != null) {

                                callbackContext.error(
                                        "Erro ao abrir scanner: "
                                                + e.getMessage()
                                );
                            }
                        }
                    })

                    .addOnFailureListener(e -> {

                        Log.e(
                                TAG,
                                "Falha ML Kit",
                                e
                        );

                        if (callbackContext != null) {

                            callbackContext.error(
                                    "Falha no ML Kit: "
                                            + e.getMessage()
                            );
                        }
                    });

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Erro startScan",
                    e
            );

            if (callbackContext != null) {

                callbackContext.error(
                        "Erro startScan: "
                                + e.getMessage()
                );
            }
        }
    }

    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        Log.d(
                TAG,
                "onActivityResult chamado"
        );

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != SCAN_REQUEST_CODE) {

            Log.d(
                    TAG,
                    "requestCode inválido"
            );

            return;
        }

        if (callbackContext == null) {

            Log.e(
                    TAG,
                    "callbackContext NULL"
            );

            return;
        }

        try {

            // Usuário finalizou scan
            if (resultCode == Activity.RESULT_OK) {

                Log.d(
                        TAG,
                        "RESULT_OK recebido"
                );

                GmsDocumentScanningResult result =
                        GmsDocumentScanningResult
                                .fromActivityResultIntent(data);

                if (result == null) {

                    Log.e(
                            TAG,
                            "Resultado NULL"
                    );

                    callbackContext.error(
                            "Resultado do scanner nulo"
                    );

                    return;
                }

                JSONObject response = new JSONObject();

                // PDF
                if (result.getPdf() != null) {

                    String pdfUri =
                            result.getPdf()
                                    .getUri()
                                    .toString();

                    Log.d(
                            TAG,
                            "PDF URI: " + pdfUri
                    );

                    response.put(
                            "pdfUri",
                            pdfUri
                    );
                }

                // Imagens
                JSONArray pagesArray = new JSONArray();

                if (result.getPages() != null &&
                        !result.getPages().isEmpty()) {

                    for (GmsDocumentScanningResult.Page page :
                            result.getPages()) {

                        String imageUri =
                                page.getImageUri()
                                        .toString();

                        pagesArray.put(imageUri);

                        Log.d(
                                TAG,
                                "IMAGE URI: " + imageUri
                        );
                    }
                }

                response.put(
                        "pages",
                        pagesArray
                );

                // Compatibilidade com seu JS atual
                if (pagesArray.length() > 0) {

                    response.put(
                            "imageUri",
                            pagesArray.getString(0)
                    );
                }

                Log.d(
                        TAG,
                        "Enviando resultado ao JS"
                );

                PluginResult pluginResult =
                        new PluginResult(
                                PluginResult.Status.OK,
                                response
                        );

                pluginResult.setKeepCallback(false);

                callbackContext.sendPluginResult(
                        pluginResult
                );
            }

            // Usuário cancelou
            else if (resultCode == Activity.RESULT_CANCELED) {

                Log.d(
                        TAG,
                        "Usuário cancelou"
                );

                callbackContext.error(
                        "Usuário cancelou o scanner"
                );
            }

            // Outro erro
            else {

                Log.e(
                        TAG,
                        "Erro desconhecido"
                );

                callbackContext.error(
                        "Erro desconhecido no scanner"
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Erro onActivityResult",
                    e
            );

            callbackContext.error(
                    "Erro ao processar resultado: "
                            + e.getMessage()
            );
        }
    }
}