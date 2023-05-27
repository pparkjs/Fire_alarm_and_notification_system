package com.example.firesensing;

// 라이브러리 import
import android.os.Bundle;  // 안드로이드SDK에서 제공하는 클래스로 이를 사용하기 위힌 import문장
import android.app.Activity; // 안드로이드SDK에서 제공하는 클래스로 이를 사용하기 위힌 import문장
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

// 메인 동작, 앱 실행 시 시작되는 코드 부분
public class MainActivity extends Activity {
    private WebView mWebView; //mWebView 인스턴스 생성
    private TextView textView1; //현재 URL을 출력할 TextView객체 생성 1
    private TextView textView2; //현재 URL을 출력할 TextView객체 생성 2

    // twilio의 SID, TOKEN 입력
    public static final String ACCOUNT_SID = "AC6a3430ae792916d255e7544ab1bbc621";
    public static final String AUTH_TOKEN = "b74b6406885810ce36cd4fc2b07a036a";

    // 앱 실행 시 생성되는 객체들
    @Override
    protected void onCreate(Bundle savedInstanceState) { //초기화면의 설정 작업을 주로 수행한다.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 서버에서 온도/상태 데이터를 갖고오기 위한 서브 스레드 생성
        backThread1 thread1 = new backThread1(); //스레드 객체를 통해서 디폴트 생성자로 생성했습니다.
        thread1.setDaemon(true); // 메인 앱 종료 시 서브 스레드도 동시에 종료하도록 설정 정수추가:setDeamon으로 메인 스레드와 종료동기화를 이룹니다. 종료 동기화는 메인스레드가 죽게 될 경우, 서브 스레드들도 같이 죽게 하는 것 입니다.
        thread1.start();         // //안그러면 서브는 메인이 종료되도 계속해서 수행하고 있을테니 그 이후 start메서드를 이용해서 스레드를 시작 시킨 것이죠 start를 이용하면 자동으로 run도 실행되게 되어 있습니다.

        backThread2 thread2 = new backThread2();
        thread2.setDaemon(true);
        thread2.start();

        // SID, 토큰을 통한 설정 초기화
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        // 웹 뷰 항목
        // 레이아웃에 선언된 Webview에 대한 인스턴스 전달
        mWebView = (WebView) findViewById(R.id.wv_motion);
        // 웹뷰에서 자바스크립트실행가능
        mWebView.getSettings().setJavaScriptEnabled(true);

        // 홈페이지 주소
        // 내부 아이피 사용시
        mWebView.loadUrl("http://192.168.0.10:8081/");
        // 외부 아이피 사용시
//        mWebView.loadUrl("1.228.170.57:54321");
        // WebViewClient 지정
        mWebView.setWebViewClient(new WebViewClientClass());

    }

    // 화재 상태 서버로부터 갖고오는 동작
    class backThread1 extends Thread{
        public void run() {
            String resultText = "[NULL]"; // 초기 문자열은 공백
            while(true) {
                // activity_main에 존재하는 상태 텍스트뷰와 연동
                textView1 = (TextView)findViewById(R.id.temp_1);  //디자인 영역에서 만든 뷰를 코드영역에서 제어하기 위해서 사용하는 기능(바인딩)!XML에서 한 개의 텍스트를 배치(temp_1)
                try { // 서버에서 값을 받아오기 위한 HTTP 요청
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .build();  //OkHttpClient -> 안드로이드에서 제공하는 라이브러리로 http요청하는거
                    Request request = new Request.Builder()
                            .url("http://203.253.128.161:7579/Mobius/Fire_sensing/Temp/la")
                            .method("GET", null)
                            .addHeader("Accept", "application/json") //addheader 웹브라우저에 보내는 응답 정보 name헤더에 value를 값으로 추가
                            .addHeader("X-M2M-RI", "12345")
                            .addHeader("X-M2M-Origin", "SOrigin")
                            .build();
                    Response response = client.newCall(request).execute();

                    // 응답으로 받아온 값을 임시 저장, 문자열에서 온도값만 추출하여 저장
                    String temp = response.body().string();
                    String temp2[] = temp.split("\"");
                    resultText = temp2[39] + "°C";
                } catch (Exception e) { // 에러 발생시 출력
                    System.err.println(e.toString());
                }
                textView1.setText(resultText); // 받아온 온도값으로 텍스트뷰 값 변경
            }
        }
    }

    // 위와 동일한 방식의 동작
    class backThread2 extends Thread{
        public void run() {
            String resultText = "[NULL]";
            while(true) {
                textView2 = (TextView)findViewById(R.id.stat_1);
                try {
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .build();
                    Request request = new Request.Builder()
                            .url("http://203.253.128.161:7579/Mobius/Fire_sensing/Fire/la")
                            .method("GET", null)
                            .addHeader("Accept", "application/json")
                            .addHeader("X-M2M-RI", "12345")
                            .addHeader("X-M2M-Origin", "SOrigin")
                            .build();
                    Response response = client.newCall(request).execute();
                    String temp = response.body().string();
                    String temp2[] = temp.split("\"");
                    resultText = temp2[39];
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
                textView2.setText(resultText);
            }
        }
    }

    // 위에서 설정된대로 웹뷰(스트리밍) 실행
    private class WebViewClientClass extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
