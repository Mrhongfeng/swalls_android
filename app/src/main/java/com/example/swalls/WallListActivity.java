package com.example.swalls;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.swalls.core.http.JsonArrayRequest;

import com.android.volley.AuthFailureError;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import com.android.volley.VolleyError;
//import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;


import com.android.volley.toolbox.Volley;
import com.example.swalls.adapter.QuestionListViewAdapter;
import com.example.swalls.constant.Const;
import com.example.swalls.core.data.converter.MultipartHttpConverter;
import com.example.swalls.core.data.converter.entity.BaseTransferEntity;
import com.example.swalls.core.data.converter.entity.SecretKeyEntity;
import com.example.swalls.core.util.JsonUtils;
import com.example.swalls.modal.College;
import com.example.swalls.modal.Edu;
import com.example.swalls.modal.Lecture;
import com.example.swalls.modal.Wall;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WallListActivity extends AppCompatActivity implements AbsListView.OnScrollListener {

    public static String URL;  //定义网络图片地址

    private String url;

    private QuestionListViewAdapter adapter;

    private ListView listView;

    private Thread mThread;

    private View convertView;

    //共享数据
    private SharedPreferences sharedPreferences;

    //Volley队列
    private RequestQueue request;

    private MultipartHttpConverter multipartHttpConverter;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qlist_list);
        Bundle bundle = getIntent().getExtras();

        //模块标题  顶栏
        ((TextView)findViewById(R.id.item_mode_name)).setText("首页");
        WallListActivity.URL = Const.URL + "/wall";
        this.url = WallListActivity.URL + "/selectByAbstracts";  //获取问题列表接口

        init();

        requestWallListInfo();
    }

    private void init() {
        //共享数据
        sharedPreferences = getSharedPreferences(Const.SECRET_KEY_DATEBASE,MODE_PRIVATE);
        //网络请求器
        request = Volley.newRequestQueue(WallListActivity.this);

        keyRefresh();

        //数据转换器
        multipartHttpConverter = new MultipartHttpConverter();

        convertView = LayoutInflater.from(this).inflate(R.layout.qlist_end,null);
        adapter = new QuestionListViewAdapter(this);
        listView = (ListView)findViewById(R.id.list);  //得到一个listView用来显示条目
        listView.addFooterView(convertView);  //添加到脚页显示
        listView.setAdapter(adapter);  //给listview添加适配器
        listView.setOnScrollListener(this);  //给listview注册滚动监听
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture){

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int tableItemCount){

    }

    /**
     * 获取问题列表
     */
    private void requestWallListInfo() {

//        mThread = new Thread() {
//            @Override
//            public void run() {
//
//                // 请求参数
//                BaseTransferEntity bte = null;
//
//                Wall wall = new Wall();
//                wall.setOpenId("ojnw95baofXIwxqN6R4PTYGytOaI");
//                wall.setAbstracts("");
//                bte = multipartHttpConverter.encryption(wall, sharedPreferences.getString("randomKey", ""));
//
//                Map<String, String> params = new HashMap<>();
//                params.put("object", bte.getObject());
//                params.put("sign", bte.getSign());
//
//                HttpUtil.sendHttpRequest(url, params, new HttpUtil.HttpCallbackListener() {
//
//                    @Override
//                    public void onSuccess(String response) {
//                        Log.i("hhf", " -> onSuccess -> " + response);
//                    }
//
//                    @Override
//                    public void onError(String info) {
//                        Log.i("hhf", " -> onError -> " + info);
//                    }
//
//                });
//            }
//        };
//        mThread.start();

        mThread = new Thread() {
            @Override
            public void run() {
                try {
                    // 请求参数
                    BaseTransferEntity bte = null;
                    Wall wall = new Wall();
                    wall.setOpenId("ojnw95baofXIwxqN6R4PTYGytOaI");
                    wall.setAbstracts("");
                    bte = multipartHttpConverter.encryption(wall, sharedPreferences.getString("randomKey",""));
                    //数据加密
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("object", bte.getObject());
                    jsonObject.put("sign", bte.getSign());
                    JsonArrayRequest stringRequest = new JsonArrayRequest(Request.Method.POST, url, jsonObject, new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            Message message = new Message();
                            message.what = 1;
                            message.obj = JsonUtils.fromListJson(jsonArray.toString(),Wall.class);
                            handler.sendMessage(message);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                            Toast.makeText(WallListActivity.this, error.toString(), Toast.LENGTH_LONG).show();
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

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState){

    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){

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
     * 获取令牌
     */
    private void keyRefresh(){
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, Const.URL, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                SecretKeyEntity sk = JsonUtils.fromJson(response, SecretKeyEntity.class);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("token",sk.getToken());
                                editor.putString("randomKey",sk.getRandomKey());
                                editor.apply();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(WallListActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                        }
                    }) {
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            // 请求参数
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("userName", "admin");
                            map.put("password", "admin");
                            return map;
                        }
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            // 请求头
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("Content-Type","application/x-www-form-urlencoded");
                            return map;
                        }
                    };
                    request.add(stringRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

}
