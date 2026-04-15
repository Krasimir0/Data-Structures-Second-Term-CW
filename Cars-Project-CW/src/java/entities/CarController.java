package entities;

import entities.util.JsfUtil;
import entities.util.PaginationHelper;

import java.io.Serializable;
import java.util.ResourceBundle;
import jakarta.annotation.Resource;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.model.DataModel;
import jakarta.faces.model.ListDataModel;
import jakarta.faces.model.SelectItem;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.UserTransaction;

@Named("carController")
@SessionScoped
public class CarController implements Serializable {

    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "Cars-Project-CWPU")
    private EntityManagerFactory emf = null;

    private Car current;
    private DataModel items = null;
    private CarJpaController jpaController = null;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private String searchQuery;

    public CarController() {
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
    
    private int generateCarId() {
    return (int)(System.currentTimeMillis() % 1_000_000);
}

    public String search() {
        recreateModel();

        items = new ListDataModel(
                getJpaController().findCarEntities()
        );

        if (searchQuery != null && !searchQuery.isEmpty()) {
            items = new ListDataModel(
                    getJpaController().findCarEntities()
                            .stream()
                            .filter(c
                                    -> c.getBrand().toLowerCase().contains(searchQuery.toLowerCase())
                            || c.getModel().toLowerCase().contains(searchQuery.toLowerCase())
                            )
                            .toList()
            );
        }

        return null;
    }

    public Car getSelected() {
        if (current == null) {
            current = new Car();
            selectedItemIndex = -1;
        }
        return current;
    }

    public void refresh() {
        items = null;
        pagination = null;
    }

    private CarJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new CarJpaController(utx, emf);
        }
        return jpaController;
    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(10) {

                @Override
                public int getItemsCount() {
                    return getJpaController().getCarCount();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getJpaController().findCarEntities(getPageSize(), getPageFirstItem()));
                }
            };
        }
        return pagination;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        current = (Car) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new Car();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            current.setCarId(generateCarId());
            current.setStatus("AVAILABLE");
                     
            getJpaController().create(current);
            
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle("/Bundle").getString("CarCreated")
            );
            
            recreateModel();
            recreatePagination();
            
            return "List?faces-redirect=true";
            
         
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareEdit() {
        current = (Car) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getJpaController().edit(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("CarUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (Car) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreatePagination();
        recreateModel();
        return "List";
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    private void performDestroy() {
        try {
            getJpaController().destroy(current.getCarId());
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("CarDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getJpaController().getCarCount();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count - 1;
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            current = getJpaController().findCarEntities(1, selectedItemIndex).get(0);
        }
    }

    public DataModel getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    public void recreateModel() {
        items = null;
    }

    public void setItems(DataModel items) {
        this.items = items;
    }

    public void recreatePagination() {
        pagination = null;
    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findCarEntities(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findCarEntities(), true);
    }

    @FacesConverter(forClass = Car.class)
    public static class CarControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CarController controller = (CarController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "carController");
            return controller.getJpaController().findCar(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Car) {
                Car o = (Car) object;
                return getStringKey(o.getCarId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + Car.class.getName());
            }
        }

    }

}
