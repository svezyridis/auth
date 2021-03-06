package sphy.subject.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import sphy.RestResponse;
import sphy.Validator;
import sphy.auth.models.NewUser;
import sphy.image.storage.FileSystemStorageService;
import sphy.image.storage.StorageException;
import sphy.image.storage.StorageFileNotFoundException;
import sphy.subject.db.CategoryRepository;
import sphy.subject.models.*;
import sphy.subject.db.SubjectRepository;


import java.util.List;


@RestController
public class SubjectController {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("jdbcSubjectRepository")
    private SubjectRepository subjectRepository;

    @Autowired
    @Qualifier("jdbcCategoryRepository")
    private CategoryRepository categoryRepository;

    @Autowired
    Validator validator;

    Logger logger = LoggerFactory.getLogger(SubjectController.class);

    @Autowired
    FileSystemStorageService storageService;

    @ExceptionHandler(StorageException.class)
    public RestResponse handleStorageFileNotFound(StorageException exc) {
        return new RestResponse("error",null,"some images could not be deleted");
    }


    /**
     * @param category name of the requested category
     * @param token
     * @return all subjects of the specified category
     */
    @RequestMapping(value = "subject/{weapon}/{category}")
    public RestResponse getSubjectByCategory(@PathVariable String category, @PathVariable String weapon, @CookieValue(value = "jwt", defaultValue = "token") String token) {
        logger.info("[SubjectController]:[getSubjectByCategory]:{category: "+category+", weapon"+weapon+" }");
        if (!validator.simpleValidateToken(token))
            return new RestResponse("error", null, "token is invalid");

        Integer weaponID = categoryRepository.getWeaponID(weapon);
        if (weaponID == -1)
            return new RestResponse("error", null, "weapon does not exist");

        Integer categoryID = categoryRepository.getCategoryID(category, weaponID);
        if (categoryID == -1)
            return new RestResponse("error", null, "category does not exist");

        List<Subject> subjects = subjectRepository.getSubjectsOfCategory(categoryID);
        for (Subject sub : subjects) {
            List<Image> images = subjectRepository.getImagesOfSubject(sub.getID());
            sub.setImages(images);
            sub.setCategory(category);
        }
        return new RestResponse("success", subjects, null);
    }
    @RequestMapping(value = "subject/{uri}")
    public RestResponse getSubjectByURI(@PathVariable String uri, @CookieValue(value = "jwt", defaultValue = "token") String token) {
        logger.info("[SubjectController]:[getSubjectByURI]:{uri: "+uri+"}");
        if (!validator.simpleValidateToken(token))
            return new RestResponse("error", null, "token is invalid");
        Subject subject = subjectRepository.getSubjectByURI(uri);
        if(subject==null)
            return new RestResponse("error", null, "subject does not exist");
            List<Image> images = subjectRepository.getImagesOfSubject(subject.getID());
            subject.setImages(images);
        return new RestResponse("success", subject, null);
    }


    @PostMapping(value = "subject/{weapon}/{category}")
    public RestResponse createSubject(@CookieValue(value = "jwt", defaultValue = "token") String token, @PathVariable String weapon, @PathVariable String category, @RequestBody Subject subject) {
        logger.info("[SubjectController]:[createSubject]:{category: "+category+", weapon: "+weapon+" subject: "+subject+" }");
        if (!validator.validateAdminToken(token))
            return new RestResponse("error", null, "token is not a valid ADMIN token");
        Integer weaponID = categoryRepository.getWeaponID(weapon);
        if (weaponID == -1)
            return new RestResponse("error", null, "weapon does not exist");

        Integer categoryID = categoryRepository.getCategoryID(category, weaponID);
        if (categoryID == -1)
            return new RestResponse("error", null, "category does not exist");

        Integer subjectID = subjectRepository.getSubjectID(subject.getURI());
        if (subjectID != -1)
            return new RestResponse("error", null, "subject already exists");
        //TODO validate subject has all required fields
        int res = subjectRepository.createSubject(subject, categoryID);
        if (res == -1)
            return new RestResponse("error", null, "subject creation failed");

        subject.setCategoryID(categoryID);
        subject.setID(res);
        subject.setCategory(category);
        return new RestResponse("success", subject, null);
    }
    @PutMapping(value = "subject/{weapon}/{category}/{subjectName}")
    public RestResponse updateSubject(@CookieValue(value = "jwt", defaultValue = "token") String token,@PathVariable String weapon, @PathVariable String category, @PathVariable String subjectName,@RequestBody Subject subject){
        logger.info("[SubjectController]:[updateSubject]:{newSubject:"+subject+" }");
        if (!validator.validateAdminToken(token))
            return new RestResponse("error", null, "token is not a valid ADMIN token");
        Integer weaponID = categoryRepository.getWeaponID(weapon);
        if (weaponID == -1)
            return new RestResponse("error", null, "weapon does not exist");

        Integer categoryID = categoryRepository.getCategoryID(category, weaponID);
        if (categoryID == -1)
            return new RestResponse("error", null, "category does not exist");

        Integer subjectID = subjectRepository.getSubjectID(subjectName);
        if (subjectID == -1)
            return new RestResponse("error", null, "subject does not exist");
        subject.setID(subjectID);
        Integer result=subjectRepository.updateSubject(subject);
        if(result==-1)
            return new RestResponse("error", null, "subject could not be updated");
        return new RestResponse("success", null, "subject updates successfully");
    }

    @DeleteMapping(value = "subject/{weapon}/{category}/{subject}")
    public RestResponse deleteSubject(@CookieValue(value = "jwt", defaultValue = "token") String token, @PathVariable String weapon, @PathVariable String category, @PathVariable String subject){
        logger.info("[SubjectController]:[deleteSubject]:{category: "+category+", weapon"+weapon+" }");
        if (!validator.validateAdminToken(token))
            return new RestResponse("error", null, "token is not a valid ADMIN token");
        Integer weaponID = categoryRepository.getWeaponID(weapon);
        if (weaponID == -1)
            return new RestResponse("error", null, "weapon does not exist");

        Integer categoryID = categoryRepository.getCategoryID(category, weaponID);
        if (categoryID == -1)
            return new RestResponse("error", null, "category does not exist");

        Integer subjectID = subjectRepository.getSubjectID(subject);
        if (subjectID == -1)
            return new RestResponse("error", null, "subject does not exist");
        Integer result=subjectRepository.deleteSubject(subjectID);
        if(result==-1)
            return new RestResponse("error", null, "subject could not be deleted");
        storageService.deleteDirectory(weapon+'/'+category+'/'+subject);
        return new RestResponse("success", null, "subject deleted successfully");
    }


}
