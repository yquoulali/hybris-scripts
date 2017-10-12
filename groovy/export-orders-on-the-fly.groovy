import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamImplicit
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.io.xml.StaxDriver
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder
import com.thoughtworks.xstream.mapper.Mapper
import de.hybris.platform.core.model.order.AbstractOrderEntryModel
import de.hybris.platform.core.model.order.OrderModel
import de.hybris.platform.core.model.product.ProductModel
import de.hybris.platform.core.model.user.CustomerModel
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery
import de.hybris.platform.servicelayer.search.SearchResult
import groovy.transform.Immutable

import java.lang.reflect.Field
import java.lang.reflect.Modifier

exportOrders()


private exportOrders(){

    Orders orders = new Orders()

    Collection<OrderModel> lastMonthOrders = this.findLastMonthOrders()
    println("Exporting " + lastMonthOrders.size() + " order")
    for (Object object : lastMonthOrders){
        OrderModel orderModel = (OrderModel) object
        Order order = new Order(
                id: orderModel.getCode(),
                creationDate: orderModel.getCreationtime(),
                products: getProducts(orderModel),
                attrs: getAttributes(orderModel)
        )
        orders.add(order)
    }

    writeXmlFile(orders, new File("/path/to/output/folder/siteId/orders.xml"))

    return "Finished"
}

private static writeXmlFile(Orders orders, File output){

    //To avoid double escapping of underscore
    def driver = new StaxDriver(new XmlFriendlyNameCoder("_-", "_"))
    XStream xstream = new XStream(driver)
    xstream.processAnnotations(Orders.class, OrdersAttributes.class);
    xstream.registerConverter(new GroovyObjectConverter(xstream.mapper))
    xstream.toXML(orders, new OutputStreamWriter(new FileOutputStream(output)))

}
OrdersAttributes getAttributes(OrderModel orderModel) {
    OrdersAttributes ordersAttributes = new OrdersAttributes(
            customerId: ((CustomerModel)orderModel.getUser()).getCustomerID(),
            status: enumerationService.getEnumerationName(orderModel.getStatus()),
            totalAti: new Float(orderModel.getTotalPrice())
    )

    return ordersAttributes
}

Products getProducts(OrderModel orderModel) {
    Products  products = new Products()
    final List<AbstractOrderEntryModel> entries = orderModel.getEntries()
    for (AbstractOrderEntryModel entry : entries){
        final ProductModel productModel = entry.getProduct()
        final Float totalPrice = new Float(entry.getTotalPrice())
        Product product = new Product(
                id: productModel.getCode(),
                qty: entry.getQuantity(),
                price: totalPrice,
                priceAti: totalPrice
        )

        products.add(product)
        
    }
    return products
}
private Collection<OrderModel> findLastMonthOrders(){
    final String query = "SELECT {" + OrderModel.PK + "}" +
            " FROM {" + OrderModel._TYPECODE + "!}" +
            " WHERE {" + OrderModel.CREATIONTIME + "} >= ?startDate" +
            " AND {" + OrderModel.CREATIONTIME +"} <= ?endDate" +
            " AND {" + OrderModel.SITE + "} = ?site"

    Calendar calY = Calendar.getInstance()

    calY.set(Calendar.YEAR, 2017)
    calY.set(Calendar.MONTH, 9)
    calY.set(Calendar.DATE, 7)
    calY.set(Calendar.MINUTE, 0)
    calY.set(Calendar.MINUTE, 0)
    calY.set(Calendar.SECOND, 0)
    calY.set(Calendar.MILLISECOND, 0)

    Calendar calT = Calendar.getInstance()

    calT.set(Calendar.YEAR, 2017)
    calT.set(Calendar.MONTH, 10)
    calT.set(Calendar.DATE, 7)
    calT.set(Calendar.MINUTE, calT.getMaximum(Calendar.MINUTE))
    calT.set(Calendar.SECOND,  calT.getMaximum(Calendar.SECOND))
    calT.set(Calendar.MILLISECOND,  calT.getMaximum(Calendar.MILLISECOND))

    Map<String, Object> params =  new HashMap<>()
    params.put("startDate", calY.getTime())
    params.put("endDate", calT.getTime())
    params.put("site", '8796093056040') //site pk

    FlexibleSearchQuery fsQuery = new FlexibleSearchQuery(query.toString(), params)

    final SearchResult<OrderModel> searchRes = flexibleSearchService.search(fsQuery)

    return searchRes.getResult()

}

class GroovyObjectConverter extends ReflectionConverter {
    GroovyObjectConverter(Mapper mapper) {
        super(mapper, new GroovyObjectReflectionProvider())
    }

    boolean canConvert(Class type) {
        GroovyObject.class.isAssignableFrom(type)
    }

}

class GroovyObjectReflectionProvider extends PureJavaReflectionProvider {
    protected boolean fieldModifiersSupported(Field field) {
        int modifiers = field.getModifiers()
        super.fieldModifiersSupported(field) && !Modifier.isSynthetic(modifiers)
    }
}

@Immutable
@XStreamAlias("orders")
class Orders {

    @XStreamImplicit(itemFieldName = "order")
    protected List<Order> orders = new ArrayList<>();

    public add(Order o){
        orders.add(o)
    }
}
class Order {
    @XStreamAlias("creation_date")
    protected Date creationDate;
    protected String id;
    protected Products products;
    protected Object attrs;
}
class Products {
    @XStreamImplicit(itemFieldName = "product")
    protected List<Product> product = new ArrayList<>();
    
    public add(Product p){
        this.product.add(p)
    }
}

class Product {
    protected String id
    protected Long qty
    @XStreamAlias("price_ati")
    protected float priceAti
    protected float price
    protected Attrs attrs

}
class Attrs {
    @XStreamAlias("date_begin")
    protected String dateBegin;
    @XStreamAlias("date_end")
    protected String dateEnd;

}
@XStreamAlias("attrs")
class OrdersAttributes {
    @XStreamAlias("customer_id")
    private String customerId;
    private String status;
    @XStreamAlias("total_ati")
    private Float totalAti;
}