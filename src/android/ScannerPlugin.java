// Resumo da lógica principal em Java
public void startScan() {
    GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
        .build();

    GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);
    
    scanner.getStartScanIntent(activity)
        .addOnSuccessListener(intentSender -> {
            // Abre a interface do Google
            activity.startIntentSenderForResult(intentSender, SCAN_REQUEST_CODE, null, 0, 0, 0);
        });
}