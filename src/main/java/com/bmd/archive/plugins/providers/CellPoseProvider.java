package com.bmd.archive.plugins.providers;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.NotImplementedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.datastructs.dim.BulkAnnotation;
import pt.ua.dicoogle.sdk.datastructs.dim.Point2D;
import pt.ua.dicoogle.sdk.mlprovider.*;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.dicoogle.sdk.task.ProgressCallable;
import pt.ua.dicoogle.sdk.task.Task;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * An ML provider for Cellpose
 */
public class CellPoseProvider extends MLProviderInterface {

    private static final Logger logger = LoggerFactory.getLogger(CellPoseProvider.class);

    private final String defaultEndpoint = "http://192.168.0.30:8082/infer";
    private String endpoint = "";
    private boolean enabled = false;
    private List<MLModelParameter> parameters;
    private ConfigurationHolder settings;

    @Override
    public void dataStore(MLDataset mlDataset) {
        throw new NotImplementedException();
    }

    @Override
    public Task<Boolean> cache(MLDicomDataset mlDicomDataset) {
        throw new NotImplementedException();
    }
    @Override
    public MLModel createModel() {
        throw new NotImplementedException();
    }

    @Override
    public MLTrainTask trainModel(String s) {
        throw new NotImplementedException();
    }

    @Override
    public boolean stopTraining(String s) {
        return false;
    }

    @Override
    public List<MLModel> listModels() {
        List<MLModel> models = new ArrayList<>();
        models.add(new MLModel("Cells", "c", parameters));
        return models;
    }

    @Override
    public MLModelTrainInfo modelInfo(String s) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteModel() {
        throw new NotImplementedException();
    }

    @Override
    public Task<MLInference> infer(MLInferenceRequest request) {
        return new Task<>(new ProgressCallable<MLInference>() {

            private float progress = 0;

            @Override
            public float getProgress() {
                return progress;
            }

            @Override
            public MLInference call() throws Exception {
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpPost httpPost = new HttpPost(endpoint);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(request.getRoi(), "jpg", baos);

                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.addBinaryBody(
                            "file", baos.toByteArray(), ContentType.IMAGE_JPEG, "image.jpeg");
                    httpPost.setEntity(builder.build());

                    CloseableHttpResponse response;

                    try {
                        response = client.execute(httpPost);
                    } catch (IOException e) {
                        logger.error("Error sending request", e);
                        throw new RuntimeException(e);
                    }

                    int statusCode = response.getStatusLine().getStatusCode();
                    if(statusCode >= HttpStatus.SC_BAD_REQUEST){
                        logger.error(String.format("Request return error %d", statusCode),
                                new Error(response.getStatusLine().getReasonPhrase()));
                        return null;
                    }

                    HttpEntity responseEntity = response.getEntity();
                    String json = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                    JSONObject o = new JSONObject(json);
                    MLInference prediction = parsePredictionJSON(o);
                    progress = 1.0f;
                    return prediction;
                } catch (IOException e) {
                    logger.error("Error creating request", e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void batchInfer() {

    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Set<MLMethod> getImplementedMethods() {
        Set<MLMethod> methods = new HashSet<>();
        methods.add(MLMethod.INFER);
        methods.add(MLMethod.LIST_MODELS);
        return methods;
    }

    @Override
    public String getName() {
        return "provider-cellpose";
    }

    @Override
    public boolean enable() {
        enabled = true;
        return true;
    }

    @Override
    public boolean disable() {
        enabled = false;
        return false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setSettings(ConfigurationHolder configurationHolder) {

        // Load configuration parameters from XML
        this.settings = configurationHolder;

        XMLConfiguration cnf = settings.getConfiguration();
        cnf.setThrowExceptionOnMissing(false);

        try {
            SubnodeConfiguration configuration = cnf.configurationAt("cellpose");
            this.endpoint = configuration.getString("baseURL", this.defaultEndpoint);
            this.enabled = configuration.getBoolean("enabled");
        } catch (Exception e) {
            logger.warn("Cellpose configuration not found, proceeding with default configurations", e);
            if(this.endpoint.isEmpty())
                this.endpoint = this.defaultEndpoint;
        }
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

    /**
     * Given a json object, parse its contents to return a MLPrediction.
     * @param json
     * @return
     */
    private MLInference parsePredictionJSON(JSONObject json) throws JSONException {
        MLInference prediction = new MLInference();

        String version = "1";
        if(json.has("version")){
            version = json.getString("version");
        }
        prediction.setVersion(version);

        HashMap<String, String> metrics = new HashMap<>();
        BulkAnnotation annotation = new BulkAnnotation(BulkAnnotation.AnnotationType.POLYGON, BulkAnnotation.PixelOrigin.FRAME);
        if(json.has("annotations")){
            JSONArray jsonAnnotations = json.getJSONArray("annotations");
            int size = jsonAnnotations.length();
            for(int i = 0; i < size; i++){
                JSONArray ann = jsonAnnotations.getJSONArray(i);
                List<Point2D> points = new ArrayList<>(ann.length());
                for(int j = 0; j < ann.length(); j++){
                    JSONArray point = ann.getJSONArray(j);
                    Point2D p = new Point2D(point.getDouble(0), point.getDouble(1));
                    points.add(p);
                }
                annotation.getAnnotations().add(points);
            }
        }
        prediction.setAnnotations(Collections.singletonList(annotation));

        return prediction;
    }
}
