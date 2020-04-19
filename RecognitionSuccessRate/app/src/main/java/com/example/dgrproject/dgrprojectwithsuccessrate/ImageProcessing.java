package com.example.dgrproject.dgrprojectwithsuccessrate;


import android.os.AsyncTask;
import org.opencv.core.Mat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ImageProcessing extends AsyncTask {
    private Mat croppingImage, sobelImage, lbpImage, scaledImage, pcaImage;
    private List<Mat> rawImageSets;
    private int width = 300, height = 400; // resimler yan olduğu için width 'i height'ten büyük olduğu için
    private MainActivity mainActivity;
    List<Mat> trainSets, testSets;
    int k = 5;
    int n;
    float toplam = 0;

    public ImageProcessing(List<Mat> rawTrainingSets, MainActivity mainActivity) {

        this.rawImageSets = rawTrainingSets;
        this.mainActivity = mainActivity;
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        try {
            updateUI(nativeFunction());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    private float nativeFunction() throws IOException {
        n = rawImageSets.size();
        pcaImage = new Mat();
        List<Mat> processedImgSets = new ArrayList<>();

        for (int i = 0; i < rawImageSets.size(); i++) {
            croppingImage = new Mat();
            scaledImage = new Mat();
            sobelImage = new Mat();
            lbpImage = new Mat();
            NativeClass.findContourAndCropping(rawImageSets.get(i).getNativeObjAddr(), croppingImage.getNativeObjAddr());
            NativeClass.scalingWithAspectRatio(croppingImage.getNativeObjAddr(), width, height);
            NativeClass.sobelFilter(croppingImage.getNativeObjAddr(), sobelImage.getNativeObjAddr());
            NativeClass.lbpWithHistogram(sobelImage.getNativeObjAddr(), lbpImage.getNativeObjAddr());

            processedImgSets.add(i, lbpImage);
            rawImageSets.get(i).release();
        }

        int chunk = n / k;
        for (int i = 0; i < k; i++) {
            int start = chunk * i;
            int end = chunk * (i + 1);
            if (i == k - 1) end = n;

            trainSets = new ArrayList<>(n - end + start);
            testSets = new ArrayList<>(end - start);

            for (int j = 0, s = 0, g = 0; j < n; j++) {
                if (j >= start && j < end) {
                    testSets.add(g++, processedImgSets.get(j));
                } else {
                    trainSets.add(s++, processedImgSets.get(j));
                }
            }
            // List<Mat>'ı Native tarafa göndermek için
            int elemsTrain = trainSets.size();

            long[] tempobjadrOfTrain = new long[elemsTrain];
            for (int p = 0; p < elemsTrain; p++) {
                Mat tempaddr = trainSets.get(p);
                tempobjadrOfTrain[p] = tempaddr.getNativeObjAddr();
            }
            int elemsTest = testSets.size();

            long[] tempobjadrOfTest = new long[elemsTest];
            for (int q = 0; q < elemsTest; q++) {
                Mat tempaddr = testSets.get(q);
                tempobjadrOfTest[q] = tempaddr.getNativeObjAddr();
            }


            //SVM
            float ortalama = NativeClass.svm(tempobjadrOfTrain, tempobjadrOfTest);

            //SVM den her bir test kümesi için dönen yüzdelik değeri toplam değişkenine atıyoruz
            toplam += ortalama;
        }

        System.out.println(toplam / k);  // toplanan yüzdelik değerleri, test edilen küme sayısına bölüyoruz.


        return toplam / k;
    }

    private void updateUI(final float successRate) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.displayOnScreen(successRate);

            }
        });
    }
}


