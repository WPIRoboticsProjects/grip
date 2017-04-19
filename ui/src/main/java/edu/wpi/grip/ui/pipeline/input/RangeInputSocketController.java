package edu.wpi.grip.ui.pipeline.input;

import edu.wpi.grip.core.events.SocketChangedEvent;
import edu.wpi.grip.core.sockets.InputSocket;
import edu.wpi.grip.ui.pipeline.SocketHandleView;
import edu.wpi.grip.ui.util.GripPlatform;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.controlsfx.control.RangeSlider;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An {@link InputSocketController} that lets the user set a high and low value in a two-element
 * list.
 */
public class RangeInputSocketController extends InputSocketController<List<Number>> {

  private final GripPlatform platform;
  private final RangeSlider slider;
  private final BooleanProperty inverted = new SimpleBooleanProperty(false);

  private static final String INVERTED_CSS
      = RangeInputSocketController.class.getResource("InvertedRangeSlider.css").toExternalForm();

  /**
   * @param socket An <code>InputSocket</code> with a domain containing two <code>Number</code>s
   *               (the min and max slider values).
   */
  @Inject
  RangeInputSocketController(SocketHandleView.Factory socketHandleViewFactory,
                             GripPlatform platform,
                             @Assisted InputSocket<List<Number>> socket) {
    super(socketHandleViewFactory, socket);
    this.platform = platform;

    final Object[] domain = socket.getSocketHint().getDomain().get();
    final List<Number> initialValue = socket.getValue().get();

    checkArgument(domain.length == 1 && domain[0] instanceof List,
        "Sliders must have a domain with a list of two numbers (min and max)");

    @SuppressWarnings("unchecked")
    final List<Number> extremes = (List<Number>) domain[0];
    checkArgument((extremes.size() == 2) && (extremes.get(0) instanceof Number) && (extremes.get(1)
            instanceof Number),
        "Sliders must have a domain with a list of two numbers (min and max)");

    checkArgument(initialValue.size() == 2, "Range sliders must contain two values (low and high)");

    final double min = extremes.get(0).doubleValue();
    final double max = extremes.get(1).doubleValue();
    final double initialLow = initialValue.get(0).doubleValue();
    final double initialHigh = initialValue.get(1).doubleValue();

    this.slider = new RangeSlider(min, max, initialLow, initialHigh);
    this.slider.setShowTickMarks(true);
    this.slider.setShowTickLabels(true);
    this.slider.setMajorTickUnit(max - min);

    // Set the socket values whenever the range changes
    this.slider.lowValueProperty().addListener(o -> {
      List<Number> value = socket.getValue().get();
      value.set(0, slider.getLowValue());

      // If the high value is also changing simultaneously, don't call setValue() twice
      if (!this.slider.isHighValueChanging()) {
        socket.setValue(value);
      }
    });

    this.slider.highValueProperty().addListener(o -> {
      List<Number> range = socket.getValue().get();
      range.set(1, slider.getHighValue());
      socket.setValue(range);
    });

    this.slider.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
      if (event.getClickCount() == 2) {
        inverted.set(!inverted.get());
      }
    });

    this.inverted.addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        this.slider.getStylesheets().add(INVERTED_CSS);
      } else {
       this.slider.getStylesheets().removeAll(INVERTED_CSS); // removeAll has no exceptions!
      }
    });
  }

  @FXML
  @Override
  public void initialize() {
    super.initialize();

    // Add a label under the slider to show the exact range
    final Label label = new Label(getLowHighLabelText());
    label.setMaxWidth(Double.MAX_VALUE);
    label.setAlignment(Pos.CENTER);
    this.slider.lowValueProperty().addListener(observable ->
        label.setText(getLowHighLabelText()));
    this.slider.highValueProperty().addListener(observable ->
        label.setText(getLowHighLabelText()));


    this.setContent(new VBox(this.slider, label));
  }

  private String getLowHighLabelText() {
    return String.format("%.0f - %.0f", this.slider.getLowValue(), this.slider.getHighValue());
  }

  @Subscribe
  public void updateSliderValue(SocketChangedEvent event) {
    if (event.isRegarding(this.getSocket())) {
      platform.runAsSoonAsPossible(() -> {
        this.slider.setLowValue(this.getSocket().getValue().get().get(0).doubleValue());
        this.slider.setHighValue(this.getSocket().getValue().get().get(1).doubleValue());
      });
    }
  }

  public interface Factory {
    RangeInputSocketController create(InputSocket<List<Number>> socket);
  }
}
