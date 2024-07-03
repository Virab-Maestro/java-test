import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.io.*;
import java.time.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final ScheduledExecutorService scheduler;
    private final Semaphore semaphore;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.objectMapper = new ObjectMapper();

        long interval = timeUnit.toMillis(1);
        this.scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();

        URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Signature", signature);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            objectMapper.writeValue(os, document);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream is = connection.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } else {
            System.err.println("Failed : HTTP error code : " + responseCode);
        }
    }

    public static class Document {
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;
        public String description;

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }

    public static void main(String[] args) {
        // Пример использования
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);
        Document doc = new Document();
        // Заполнение doc необходимыми данными
        try {
            api.createDocument(doc, "your_signature_here");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
