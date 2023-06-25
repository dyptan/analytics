package com.dyptan;

import com.dyptan.model.Filter;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
/// Integration tests suite
public class SearchTest {


    public static Filter getDummyFilter(){
        Filter filter = new Filter();
        filter.setModels("[{\"Mitsubishi\", \"Saab\"}]");
        filter.setPriceFrom(Integer.valueOf("0"));
        filter.setPriceTo(Integer.valueOf("6000"));
        filter.setYearFrom(Short.valueOf("2000"));
        filter.setYearTo(Short.valueOf("2006"));
        filter.setLimit(Short.valueOf("100"));
        filter.setBrands("Dadi");
        filter.setPeriodRange(1);
        filter.setPeriodMultiplier(Filter.Period.WEEKS);
        return filter;
    }


    @Ignore
    @Test
    public void indexElasticWithFilter() {
        String jsonObject = "{\n" +
                "  \"@timestamp\": \"2018-10-20T17:41:05.171Z\",\n" +
                "  \"race_km\": \"69000\",\n" +
                "  \"price_usd\": \"5400\",\n" +
                "  \"message\": \"Модель: Shuttle, Модифікація: Suv , Рік випуску: 2006 , Тип кузова: Позашляховик / Кроссовер, Kолір: Чорний, Вид палива: Бензин, Об'єм двигуна: 22 см³, Коробка передач: Механічна, Пробіг: 69 000 км, Стан машини: Гаражне зберігання, Додаткові опції: Електросклопiдйомники, Бортовий комп'ютер, Підсилювач керма, Мультимедіа: USB, Безпека: ABS, Розмитнена: Так, Ціна: 5 400 $, <br/>Продам свой рамный джип 4*2, Аналог Toyota Land Cruiser Prado <br />\\r\\nв хорошем состоянии.  Задний привод.<br />\\r\\nПобег 69000тысяч, объём 2.2 куб. см. <br />\\r\\nСалон чистый. Комплект дисков(Германия) + резина в подарок! <br />\\r\\nВсе вопросы по телефону 0503249857 <a href=\\\"https://www.olx.ua/uk/obyavlenie/prodam-dadi-shuttle-IDC0QDM.html\\\">https://www.olx.ua/uk/obyavlenie/prodam-dadi-shuttle-IDC0QDM.html</a><img src=\\\"https://apollo-ireland.akamaized.net:443/v1/files/26taztzx5yd12-UA/image;s=94x72\\\">\",\n" +
                "  \"published\": \"2018-10-20T15:29:57.000Z\",\n" +
                "  \"title\": \"Продам Dadi Shuttle\",\n" +
                "  \"year\": \"2006\",\n" +
                "  \"model\": \"Shuttle\",\n" +
                "  \"category\": \"Dadi\",\n" +
                "  \"link\": \"https://www.olx.ua/uk/obyavlenie/prodam-dadi-shuttle-IDC0QDM.html\",\n" +
                "  \"engine_cubic_cm\": \"22\"\n" +
                "}";

        IndexRequest indexRequest = new IndexRequest("olx", "test", "1");
        indexRequest.source(jsonObject, XContentType.JSON);

        IndexResponse response = null;

        try {
            response = connector.getClient().index(indexRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String id = response.getId();
        String index = response.getIndex();
        String type = response.getType();

        assertEquals("1", id);
        assertEquals("olx", index);
        assertEquals("test", type);

        Filter filter = getDummyFilter();

        List<Map<String, Object>> documents = search.getHitsAsList(filter);

        for (Map<String, Object> doc:documents
        ) {
            System.out.println(doc.get("model"));
            System.out.println(doc.get("category"));
            assertEquals("Dadi", doc.get("category"));
            assertEquals("Shuttle", doc.get("model"));
        }
    }

    @Ignore
    @Test
    public void getAllBrands(){
        Set<String> brands = search.getBrands();
        for (String brand:brands
             ) {
            System.out.println(brand);
        }
    }

    @Ignore
    @Test
    public void getAllData(){
        SearchHit[] data = search.getAllHits(new SearchRequest("olx"));
        Arrays.stream(data)
                .map(x->x.getSourceAsString())
                .limit(5)
                .forEach(System.out::println);
    }
    @Ignore
    @Test
    public void getFilteredData(){
        SearchHit[] data = search.getFilteredHits(getDummyFilter());
        Arrays.stream(data)
                .map(x->x.getSourceAsString())
                .limit(5)
                .forEach(System.out::println);
    }

    @Ignore
    @Test
    public void getAllSearchHits() {
        SearchResponse response = null;
        try {
            response = connector.getClient().search(new SearchRequest("olx"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("All hits: "+response.getHits().getHits().length);
    }
}

