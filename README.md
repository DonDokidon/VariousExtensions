#Various extensions for java and android

##Android:

###**Drawable:**
###ExtendedAnimationDrawable:
**AnimationDrawable** child class which provides methods to work with animation:
    *Add listener and other interactions with animator</li>
    *Reverse animation from any current state</li>
 
###**Transition:**
###SharedElementTransition:
Transition set which contains main transitions for shared transition.
Changes:
    *In all contained transitions fixed bug when shared transition doesn't work if fragment change is result of **FragmentTransaction#show(Fragment)** and **FragmentTransaction#hide(Fragment)** instead of **FragmentTransaction#replace(int, Fragment)**.
    *In **ChangeTransform** for **Build.VERSION_CODES#Q** fixed bug when attached overlay view doesn't detach after animation.
    *For **ChangeTransform** added feature to draw itself behind some views
  
###Wait:
Transition to make view remain visible until animation end.
 
###**View:**
###ActionMode:
Similar to **android.view.ActionMode** but with a bit more control on ActionMode state
    
###ParallaxBackgroundScrollView & ParallaxBackgroundNestedScrollView:
**ScrollView** which allows to set several background **Bitmap** and move them with every change of scroll positions to achieve parallax effect.

###BitmapCreator:
Class to create scaled **Bitmap** and copy **Bitmap** into another **Bitmap**.
 
###**No category**:
###MotionController:
Class to handle scroll. Provides methods to:
    *Store start and current position of pointer.
    *Determine scroll direction
    *Get offset between start and current positions
    *Emulate scroll in passed {@link Direction}
    *Set interpolator for emulated scroll
  
###PagerAdapterHandler:
Class to make your Object work with large data set. Use it to provide your Object functional to change according current adapter position.
   
   
   
##Java:

###**No category**:
###FilterableArrayList:
Extended ArrayList class which provide methods to filter content without changes in real data set.


###MultiCompare:
Class that allows you to sort by multiple attributes.
 
###MultiSelectHandler:
Class to store selections
