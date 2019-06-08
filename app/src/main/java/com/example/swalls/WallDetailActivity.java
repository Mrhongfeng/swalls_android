package com.example.swalls;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.swalls.adapter.AnswerListViewAdapter;
import com.example.swalls.constant.Const;
import com.example.swalls.core.data.converter.MultipartHttpConverter;
import com.example.swalls.core.data.converter.entity.BaseTransferEntity;
import com.example.swalls.core.http.JsonArrayRequest;
import com.example.swalls.core.util.JsonUtils;
import com.example.swalls.modal.Wall;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WallDetailActivity extends AppCompatActivity {

    private final String url = WallListActivity.URL + "/showQuestion";
    private TextView tv_title;
    private TextView tv_time;
    private TextView tv_content;
    private Thread mThread;

    private final String Answerurl = WallListActivity.URL + "/showAnswers";
    private AnswerListViewAdapter adapter;
    private ListView listView;
    private Thread mThreadAnswer;
    private View convertView;

    //共享数据
    private SharedPreferences sharedPreferences;

    //Volley队列
    private RequestQueue request;

    private MultipartHttpConverter multipartHttpConverter;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qlist_detail);
        init();
        tv_title = (TextView)findViewById(R.id.wall_title);
        tv_content = (TextView)findViewById(R.id.wall_content);
        tv_content.setMovementMethod(ScrollingMovementMethod.getInstance());

        Bundle bundle = getIntent().getExtras();
        if(bundle!=null){
            requestWallInfo(bundle.getLong("id"));
            requestAnswerList(bundle.getLong("id"));
        }
    }

    private void init(){
        //共享数据
        sharedPreferences = getSharedPreferences(Const.SECRET_KEY_DATEBASE,MODE_PRIVATE);
        //网络请求器
        request = Volley.newRequestQueue(WallDetailActivity.this);
        //数据转换器
        multipartHttpConverter = new MultipartHttpConverter();

//        convertView = LayoutInflater.from(this).inflate(R.layout.qlist_end,null);
        adapter = new AnswerListViewAdapter(this);
        listView = (ListView)findViewById(R.id.list);  //得到一个listView用来显示条目
//        listView.addFooterView(convertView);  //添加到脚页显示
        listView.setAdapter(adapter);  //给listview添加适配器
//        listView.setOnScrollListener(this.listView);  //给listview注册滚动监听
    }

    /**
     * 传递参数
     */
    @SuppressLint("Handlerleak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            try{
                switch (msg.what){
                    case 1:
                        Wall data = JsonUtils.fromJson(msg.obj.toString(), Wall.class);
                        tv_title.setText(data.getAbstracts());
//                        tv_time.setText(data.getWriterTime());
                        tv_content.setText(data.getWriteContests());
                        break;
                    default:
                        break;
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    };

    /**
     * 获取问题
     */
    private void requestWallInfo(final Long id){
        mThread = new Thread() {
            @Override
            public void run() {
                try {
                    // 请求参数
                    BaseTransferEntity bte = null;

                    Wall wall  = new Wall();

                    wall.setId(id);
                    bte = multipartHttpConverter.encryption(wall, sharedPreferences.getString("randomKey",""));

                    //数据加密
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("object", bte.getObject());
                    jsonObject.put("sign", bte.getSign());
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            Message message = new Message();
                            message.what = 1;
                            message.obj = jsonObject;
                            handler.sendMessage(message);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(WallDetailActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                        }
                    }) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            // 请求头
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("Authorization","Bearer " + sharedPreferences.getString("token",""));
                            map.put("Content-Type","application/json");
                            return map;
                        }
                    };
                    request.add(jsonObjectRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        mThread.start();
    }

    @SuppressLint("HandlerLeak")
    private Handler handlerAnswer = new Handler(){

        @Override
        public void handleMessage(Message msg){

            System.out.println(msg.obj);

            List<Wall> data = (List<Wall>)msg.obj;
            switch (msg.what){
                case 1:
                    if(adapter.getCount() < data.size()){
                        adapter.setData(data);
                        Log.i("适配器数据注入 ", "数据大小: " + adapter.getCount());
                    }else{
                        listView.removeFooterView(convertView);
                    }

                    //重新刷新listview的adapter里面数据
                    adapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 获取回答列表
     */
    private void requestAnswerList(final Long id) {

        mThread = new Thread() {
            @Override
            public void run() {
                try {
                    // 请求参数
                    BaseTransferEntity bte = null;
                    Wall wall = new Wall();
//                    wall.setOpenId("ojnw95baofXIwxqN6R4PTYGytOaI");
//                    wall.setAbstracts("");
                    wall.setId(id);
                    bte = multipartHttpConverter.encryption(wall, sharedPreferences.getString("randomKey",""));
                    //数据加密
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("object", bte.getObject());
                    jsonObject.put("sign", bte.getSign());
                    JsonArrayRequest stringRequest = new JsonArrayRequest(Request.Method.POST, Answerurl, jsonObject, new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            Message message = new Message();
                            message.what = 1;
                            message.obj = JsonUtils.fromListJson(jsonArray.toString(),Wall.class);
                            handlerAnswer.sendMessage(message);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                            Toast.makeText(WallDetailActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                        }
                    })
                    {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            // 请求头
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("Authorization","Bearer " + sharedPreferences.getString("token",""));
                            map.put("Content-Type","application/json");
                            return map;
                        }
                    };
                    request.add(stringRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        mThread.start();

    }
}
